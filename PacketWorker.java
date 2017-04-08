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
    while( !done.value ) {
      for( int i = 0; i < numSources; i++ ) {
        if( uniformBool )
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
  /**
   * Add your fields and constructor here.
   */
  
  public void run() {
    //TODO: Implement me!
  }
  
  /**
   * Add any helper methods here.
   */
}
