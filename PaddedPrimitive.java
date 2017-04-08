

class PaddedPrimitive<T> {
  private long[] pad1;
  volatile T value;
  private long[] pad2;
  
  public PaddedPrimitive(T value) {
    pad1 = new long[8]; // one cache line worth of data
    this.value = value;
    pad2 = new long[8]; // on either side of the precious variable...
  }
}

class PaddedPrimitiveNonVolatile<T> {
  private long[] pad1;
  T value;
  private long[] pad2;
  
  public PaddedPrimitiveNonVolatile(T value) {
    pad1 = new long[8];
    this.value = value;
    pad2 = new long[8];
  }
}

