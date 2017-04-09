import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

public interface PacketWorker extends Runnable {
    public void run();
}

class SerialPacketWorker implements PacketWorker {
    PaddedPrimitiveNonVolatile<Boolean> done;
    final PacketSource pkt;
    final Fingerprint residue = new Fingerprint();
    long fingerprint = 0;
    long totalPackets = 0;
    final int numSources;
    final boolean uniformBool;

    public SerialPacketWorker(
            PaddedPrimitiveNonVolatile<Boolean> done,
            PacketSource pkt,
            boolean uniformBool,
            int numSources) {
        this.done = done;
        this.pkt = pkt;
        this.uniformBool = uniformBool;
        this.numSources = numSources;
    }

    public void run() {
        Packet tmp;
        while (!done.value) {
            for (int i = 0; i < numSources; i++) {
                if (uniformBool)
                    tmp = pkt.getUniformPacket(i);
                else
                    tmp = pkt.getExponentialPacket(i);
                totalPackets++;
                fingerprint += residue.getFingerprint(tmp.iterations, tmp.seed);
            }
        }
    }
}

class ParallelPacketWorker implements PacketWorker {
    public enum Strategy {
        LockFree,
        HomeQueue,
        RandomQueue,
        LastQueue
    }

    PaddedPrimitiveNonVolatile<Boolean> done;
    private final Fingerprint residue = new Fingerprint();
    private long fingerprint = 0;
    private WaitFreeQueue<Packet>[] queues;
    private Strategy strategy;
    private int workerNum;
    private Lock[] locks;
    private Random random;
    private Integer lastQueueIndex;

    public ParallelPacketWorker(
            PaddedPrimitiveNonVolatile<Boolean> done,
            WaitFreeQueue<Packet>[] queues,
            Strategy strategy,
            int workerNum,
            Lock[] locks
    ) {
        this.done = done;
        this.queues = queues;
        this.strategy = strategy;
        this.workerNum = workerNum;
        System.out.print(this.workerNum);
        random = new Random();
        lastQueueIndex = null;
        this.locks = locks;
    }

    private Packet getPacket() {
        if (strategy == Strategy.LockFree) {
            while (!done.value) {
                try {
                    return queues[workerNum].deq();
                } catch (EmptyException e) {
                }
            }
        } else if (strategy == Strategy.HomeQueue) {
            locks[workerNum].lock();
            while (!done.value) {
                try {
                    Packet packet = queues[workerNum].deq();
                    locks[workerNum].unlock();
                    return packet;
                } catch (EmptyException e) {
                }
            }
        } else if (strategy == Strategy.RandomQueue) {
            int i;
            while (!done.value) {
                i = random.nextInt(queues.length);
                locks[i].lock();
                try {
                    Packet packet = queues[i].deq();
                    locks[i].unlock();
                    return packet;
                } catch (EmptyException e) {
                }
                locks[i].unlock();
            }
        } else if (strategy == Strategy.LastQueue) {
            while (!done.value) {
                while (lastQueueIndex == null && !done.value) {
                    lastQueueIndex = random.nextInt(queues.length);
                    if (locks[lastQueueIndex].isContended()) {
                        lastQueueIndex = null;
                    }
                }
                try {
                    locks[lastQueueIndex].lock();
                    Packet packet = queues[lastQueueIndex].deq();
                    locks[lastQueueIndex].unlock();
                    return packet;
                } catch (EmptyException e) {
                    locks[lastQueueIndex].unlock();
                }
            }
        }
        // gets here at end
        return null;
    }

    public void run() {
        Packet tmp;
        while (!done.value) {
            tmp = getPacket();
            if (tmp != null) {
                fingerprint += residue.getFingerprint(tmp.iterations, tmp.seed);
            }
        }
    }
}
