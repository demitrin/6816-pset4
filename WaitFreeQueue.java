class WaitFreeQueue<T> {
  /**
   * Add your fields here
   */
  @SuppressWarnings({"unchecked"})
  public WaitFreeQueue(int capacity) {
    //TODO: Implement me!
  }
  public void enq(T x) throws FullException {
    //TODO: Implement me!
  }
  public T deq() throws EmptyException {
    //TODO: Implement me!
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
