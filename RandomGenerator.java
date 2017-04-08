import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.lang.Object;
import java.lang.Math;

// test app to demonstrate how work is distributed between uniform and 
// exponential packets
class RandomGeneratorApp {
  public static void main(String[] args) {
    int numRand = Integer.parseInt( args[0] );
    long mean = Long.parseLong( args[1] );
    int numSources = Integer.parseInt( args[2] );
    final short TEST_SEED = 23412;
    PacketSource pkt = new PacketSource(mean, numSources, TEST_SEED);
    long tmpUniform = 0;
    long tmpExponential = 0;
    Packet tmp;
    for(int i = 0; i < numSources; i++) {
      for(int j = 0; j < numRand; j++) {
        tmp = pkt.getUniformPacket(i);
        tmpUniform += tmp.iterations;
        tmp = pkt.getExponentialPacket(i);
        tmpExponential += tmp.iterations;
      }
    }
    System.out.println("Uniform vs. Exponential, by source");
    for(int i = 0; i < numSources; i++) {
      System.out.println(pkt.getUniformCount(i) + " vs. " + pkt.getExponentialCount(i));
    }
    System.out.println("........total.........");
    System.out.println(tmpUniform + " vs. " + tmpExponential);
  }
}

class Packet {
  long iterations;
  long seed;
  public Packet() {
    iterations = 0;
    seed = 0;
  }
  public Packet(long startIterations, long startSeed) {
    iterations = startIterations;
    seed = startSeed;
  }
}

class PacketSource {
  UniformGenerator uniformGen[];
  ExponentialGenerator exponentialGen[];
  UniformGenerator uniformSeed[];
  UniformGenerator exponentialSeed[];
  long exponentialMeans[];
  long uniformCount[];
  long exponentialCount[];
  public PacketSource( long mean, int numSources, short seed ) {
    uniformCount = new long[numSources];
    exponentialCount = new long[numSources];
    exponentialMeans = new long[numSources];
    uniformGen = new UniformGenerator[numSources];
    exponentialGen = new ExponentialGenerator[numSources];
    uniformSeed = new UniformGenerator[numSources];
    exponentialSeed = new UniformGenerator[numSources];
    ExponentialGenerator meanGen = new ExponentialGenerator(mean);
    for( short i = 0; i < seed; i++ )
      meanGen.getRand();
    long tmpMean = 0;
    for( int i = 0; i < numSources; i++) {
      exponentialMeans[i] = meanGen.getRand();
      tmpMean += exponentialMeans[i];
    }
    tmpMean -= (mean*numSources);
    tmpMean /= numSources;
    final long BIG_NUM = 100000000;
    for( int i = 0; i < numSources; i++) {
      uniformGen[i] = new UniformGenerator(2*mean);
      uniformGen[i].setSeed((byte)(1+i));
      uniformSeed[i] = new UniformGenerator(BIG_NUM);
      uniformSeed[i].setSeed((byte)(1+i));
      exponentialGen[i] = new ExponentialGenerator(exponentialMeans[i]-tmpMean);
      exponentialGen[i].setSeed((byte)(1+i));
      exponentialSeed[i] = new UniformGenerator(BIG_NUM);
      exponentialSeed[i].setSeed((byte)(1+i));
      uniformCount[i] = 0;
      exponentialCount[i] = 0;
      for( short j = 0; j < seed; j++ ) {
        uniformSeed[i].getRand();
        exponentialSeed[i].getRand();
      }
    }
  }
  Packet getUniformPacket(int sourceNum) {
    Packet tmp = new Packet(uniformGen[sourceNum].getRand(), uniformSeed[sourceNum].getRand());
    uniformCount[sourceNum] += tmp.iterations;
    return tmp;
  }
  Packet getExponentialPacket(int sourceNum) {
    Packet tmp = new Packet(exponentialGen[sourceNum].getRand(), exponentialSeed[sourceNum].getRand());
    exponentialCount[sourceNum] += tmp.iterations;
    return tmp;
  }
  long getUniformCount(int sourceNum) {
    return uniformCount[sourceNum];
  }
  long getExponentialCount(int sourceNum) {
    return exponentialCount[sourceNum];
  }
}

class UniformGenerator {
  long maxValue;
  RandomGenerator randGen = new RandomGenerator();
  public UniformGenerator(long startMaxValue) {
    maxValue = startMaxValue + 1;
  }
  long getRand() {
    return ( randGen.getRand()  % maxValue );
  }
  void setSeed(byte startSeed) {
    randGen.setSeed(startSeed);
  }
}

class ExponentialGenerator {
  double mean;
  RandomGenerator randGen = new RandomGenerator();
  public ExponentialGenerator(long startMean) {
    mean = startMean;
  }
  long getRand() {
    double tmpCDF = (double) (randGen.getRand() & 0xFFFFFFFF);
    tmpCDF = tmpCDF / 4294967295.0; //0xFFFFFFFF in decimal
    return (long) Math.ceil( -mean*Math.log(1.0-tmpCDF) );
  }
  void setSeed(byte startSeed) {
    randGen.setSeed(startSeed);
  }
}

class RandomGenerator {
  String str = "This is going to be my randomization string";
  byte randBytes[] = str.getBytes();
  byte seed = (byte) 1;
  Checksum checksum = new CRC32();
  public RandomGenerator() {
    checksum.update(randBytes,0,randBytes.length);
  }
  long getRand() {
    checksum.update(seed);
    return checksum.getValue();
  }
  void setSeed(byte startSeed) {
    seed = startSeed;
  }
}

