import java.util.concurrent.atomic.AtomicLong;

class SerialPacket {
    public static void main(String[] args) {

        final int numMilliseconds = Integer.parseInt(args[0]);
        final int numSources = Integer.parseInt(args[1]);
        final long mean = Long.parseLong(args[2]);
        final boolean uniformFlag = Boolean.parseBoolean(args[3]);
        final short experimentNumber = Short.parseShort(args[4]);

        @SuppressWarnings({"unchecked"})
        StopWatch timer = new StopWatch();
        PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);
        PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
        PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

        SerialPacketWorker workerData = new SerialPacketWorker(done, pkt, uniformFlag, numSources);
        Thread workerThread = new Thread(workerData);

        workerThread.start();
        timer.startTimer();
        try {
            Thread.sleep(numMilliseconds);
        } catch (InterruptedException ignore) {
            ;
        }
        done.value = true;
        memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
        try {                   // which means that done.value is visible to the workers
            workerThread.join();
        } catch (InterruptedException ignore) {
            ;
        }
        timer.stopTimer();
        final long totalCount = workerData.totalPackets;
        System.out.println("count: " + totalCount);
        System.out.println("time: " + timer.getElapsedTime());
        System.out.println(totalCount / timer.getElapsedTime() + " pkts / ms");
    }
}

class Dispatcher implements Runnable {

    private WaitFreeQueue<Packet>[] queues;
    private int numSources;
    private PacketSource pkt;
    private boolean uniformFlag;
    private PaddedPrimitiveNonVolatile<Boolean> done;
    AtomicLong totalPackets;

    public Dispatcher(
            WaitFreeQueue<Packet>[] queues,
            int numSources,
            boolean uniformFlag,
            PaddedPrimitiveNonVolatile<Boolean> done,
            PacketSource pkt,
            AtomicLong totalPackets
    ) {
        this.queues = queues;
        this.done = done;
        this.numSources = numSources;
        this.uniformFlag = uniformFlag;
        this.pkt = pkt;
        this.totalPackets = totalPackets;
    }

    public void run() {
        Packet tmp;
        while (!done.value) {
            for (int i = 0; i < numSources; i++) {
                if (uniformFlag)
                    tmp = pkt.getUniformPacket(i);
                else
                    tmp = pkt.getExponentialPacket(i);
                while (!done.value) {
                    try {
                        queues[i].enq(tmp);
                        break;
                    } catch (FullException e) {
                    }
                }
            }
            totalPackets.incrementAndGet();
        }
    }
}

class ParallelPacket {
    @SuppressWarnings({"unchecked"})
    public static void main(String[] args) {

        final int numMilliseconds = 2000;
        final boolean uniformFlag = true;
        final short experimentNumber = 5;
        final int queueDepth = 8;
        AtomicLong totalPackets = new AtomicLong(0);
        int lockType;
        short strategy;
        int sources[] = {1, 2, 8};
        long means[] = {1000, 2000, 4000, 8000};
        for (int numSources: sources) {
            System.out.println("Num sources " + numSources);
            for (long mean: means) {
                for (int j = 0; j < 3; j++) {
                    lockType = j;
                    for (short k = 0; k < 3; k++) {
                        strategy = k;
                        LockAllocator la = new LockAllocator();
                        Lock[] locks = new Lock[numSources];
                        WaitFreeQueue<Packet>[] queues = new WaitFreeQueue[numSources];
                        for (int i = 0; i < numSources; i++) {
                            locks[i] = la.getLock(lockType);
                            queues[i] = new WaitFreeQueue<>(queueDepth);
                        }
                        ParallelPacketWorker.Strategy strat;
                        if (strategy == 0) {
                            strat = ParallelPacketWorker.Strategy.LockFree;
                        } else if (strategy == 1) {
                            strat = ParallelPacketWorker.Strategy.RandomQueue;
                        } else if (strategy == 2) {
                            strat = ParallelPacketWorker.Strategy.LastQueue;
                        } else {
                            System.out.println("defaulting strat");
                            strat = ParallelPacketWorker.Strategy.LockFree;
                        }

                        StopWatch timer = new StopWatch();
                        PacketSource pkt = new PacketSource(mean, numSources, experimentNumber);
                        //
                        // Allocate and initialize locks and any signals used to marshal threads (eg. done signals)
                        PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
                        PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
                        //
                        // Allocate and initialize Dispatcher and Worker threads
                        //
                        Dispatcher dispatcher = new Dispatcher(queues, numSources, uniformFlag, done, pkt, totalPackets);
                        Thread[] workers = new Thread[numSources];
                        for (int i = 0; i < numSources; i++) {
                            ParallelPacketWorker worker = new ParallelPacketWorker(
                                    done,
                                    queues,
                                    strat,
                                    i,
                                    locks
                            );
                            workers[i] = new Thread(worker);
                            workers[i].start();
                        }
                        // call .start() on your Workers
                        //
                        timer.startTimer();
                        //
                        // call .start() on your Dispatcher
                        //
                        Thread dispatcherThread = new Thread(dispatcher);
                        dispatcherThread.start();
                        try {
                            Thread.sleep(numMilliseconds);
                        } catch (InterruptedException ignore) {
                            ;
                        }
                        //
                        // assert signals to stop Dispatcher - remember, Dispatcher needs to deliver an
                        // equal number of packets from each source
                        //
                        // call .join() on Dispatcher
                        //
                        // assert signals to stop Workers - they are responsible for leaving the queues
                        // empty - use whatever protocol you like, but one easy one is to have each
                        // worker verify that it's corresponding queue is empty after it observes the
                        // done signal set to true
                        //
                        // call .join() for each Worker
                        done.value = true;
                        memFence.value = true;  // memFence is a 'volatile' forcing a memory fence
                        try {
                            dispatcherThread.join();
                        } catch (InterruptedException ignore) {
                        }
                        for (int i = 0; i < numSources; i++) {
                            try {
                                workers[i].join();
                            } catch (InterruptedException ignore) {
                            }
                        }
                        timer.stopTimer();
                        final long totalCount = totalPackets.get();
                        System.out.println(locks[0]);
                        System.out.println(strat);
                        System.out.println("mean: " + mean);
                        System.out.println("count: " + totalCount);
                        System.out.println("time: " + timer.getElapsedTime());
                        System.out.println(totalCount / timer.getElapsedTime() + " pkts / ms");
                        System.out.println("=================================================");
                    }
                }

            }
            //
            // Allocate and initialize your Lamport queues
            //

        }
    }
}