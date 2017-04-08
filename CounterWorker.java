
class CounterStruct {
	long counter = 0;
}

class CounterWorker implements Runnable {
  final Lock lock;
  PaddedPrimitive<CounterStruct> counter;
  PaddedPrimitiveNonVolatile<Boolean> done;
  long count = 0; // this is how many times this Worker has incremented the counter
  long repeatCount = 0; // this is how many times the Worker incremented twice back-to-back
  private long lastValue = 0;
  public CounterWorker( 
    PaddedPrimitive<CounterStruct> counter, 
    PaddedPrimitiveNonVolatile<Boolean> done,
    Lock lock) 
  {
    this.lock = lock;
    this.counter = counter;
    this.done = done;
  }

  public void run() {
		int i;
    while( !done.value ) {
      long tmp;
      //  Critical Section
      //  ----------------
      lock.lock();
      tmp = counter.value.counter;
      counter.value.counter++;
      lock.unlock();
      // ------------------
      if( tmp == lastValue )
        repeatCount++;
      lastValue = tmp + 1;
      count++;
    }
  }  
}

class SoloCounterWorker implements Runnable {
  PaddedPrimitive<CounterStruct> counter;
  PaddedPrimitiveNonVolatile<Boolean> done;
  private long lastValue = 0;
  long count = 0;
  long repeatCount = 0;
  public SoloCounterWorker(
    PaddedPrimitive<CounterStruct> counter,
    PaddedPrimitiveNonVolatile<Boolean> done) 
  {
    this.counter = counter;
    this.done = done;
  }
  
  public void run() {
		int i;
    while( !done.value ) {
      long tmp;
      tmp = counter.value.counter;
      counter.value.counter++;
      if( tmp == lastValue )
        repeatCount++;
      lastValue = tmp + 1;
      count++;
		}
  }
}

