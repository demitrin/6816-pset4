
// from java.util.Random
class Fingerprint {
  final long m = (long) 0xFFFFFFFFFFFFL;
  final long a = 25214903917L;
  final long c = 11L;
  long getFingerprint(long iterations, long startSeed) {
    long seed = startSeed;
    for(long i = 0; i < iterations; i++) {
      seed = (seed*a + c) & m;
    }
    return ( seed >> 12 );
  }
}