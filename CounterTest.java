//import java.util.lang.*;
// This application launches a single worker who implements a counter with no locks
class SerialCounter {
  public static void main(String[] args) {  
    final int numMilliseconds = Integer.parseInt(args[0]);
    
    StopWatch timer = new StopWatch();
    PaddedPrimitive<CounterStruct> counter = new PaddedPrimitive<CounterStruct>(new CounterStruct());
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    Thread workerThread = new Thread(new SoloCounterWorker(counter,done), new String("SoloWorker"));
    workerThread.start();
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    timer.stopTimer();
    final long totalCount = counter.value.counter;
    try {
      workerThread.join();
    } catch (InterruptedException ignore) {;}
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " inc / ms");
  }  
}

// This application launches numThreads workers who try to lock the counter and increment it
class ParallelCounter {
  public static void main(String[] args) {
    final int numMilliseconds = Integer.parseInt(args[0]);
    final int numThreads = Integer.parseInt(args[1]);
    final int lockType = Integer.parseInt(args[2]);
		
    PaddedPrimitive<CounterStruct> counter = new PaddedPrimitive<CounterStruct>(new CounterStruct());
    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    StopWatch timer = new StopWatch();
    Lock lock;
    
    LockAllocator la = new LockAllocator();
    lock = la.getLock(lockType);
    la.printLockType(lockType);
    
    lock.lock(); // I'll grab the lock and then later unlock as I release the workers

    Thread[] workerThread = new Thread[numThreads];
    CounterWorker[] workerData = new CounterWorker[numThreads];

    for( int i = 0; i < numThreads; i++ ) {
      workerData[i] = new CounterWorker(counter,done,lock);
      workerThread[i] = new Thread(workerData[i], new String("Worker"+i));
    }

    for( int i = 0; i < numThreads; i++ ) 
      workerThread[i].start();
    
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {;}
    
    lock.unlock(); // release the hounds
    timer.startTimer();
    try {
      Thread.sleep(numMilliseconds); // wait for a while
    } catch (InterruptedException e) {;}
    lock.lock(); // stop the madness
    timer.stopTimer(); // measure the throughput...
    done.value = true;
    final long totalCount = counter.value.counter;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " inc / ms");

    lock.unlock(); // give the workers a chance to see done.value == true
    
    long[] count = new long[numThreads];
    for( int i = 0; i < numThreads; i++ ) {
      try {
        workerThread[i].join();
        count[i] = workerData[i].count; // collect their independent counts
        //System.out.println(count[i]);
      } catch (InterruptedException ignore) {;}      
    }
    //System.out.println(counter.value.counter);
    System.out.println(Statistics.getStdDev(count));
  }  
}
