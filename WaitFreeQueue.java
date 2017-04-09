class WaitFreeQueue<T> {
  private volatile int head = 0;
  private volatile int tail = 0;
  private T[] items;
  @SuppressWarnings({"unchecked"})
  public WaitFreeQueue(int capacity) {
    items = (T[]) new Object[capacity];
  }
  public void enq(T x) throws FullException {
    if (tail - head == items.length) {
      throw new FullException();
    }
    items[tail % items.length] = x;
    tail++;
  }
  public T deq() throws EmptyException {
      if (tail - head == 0) {
        throw new EmptyException();
      }
      T x = items[head % items.length];
      head++;
      return x;
  }
}


class FullException extends Exception {
  private static final long serialVersionUID = 1L;
  public FullException() {
    super();
  } 
}

class EmptyException extends Exception {
  private static final long serialVersionUID = 1L;
  public EmptyException() {
    super();
  } 
}
