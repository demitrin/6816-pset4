
public class Statistics {

  public static double getStdDev(long[] count) {  // calculates the std dev of an array
    double sum = 0;                               // of longs
    double sumSquared = 0;
    for(int i = 0; i < count.length; i++) {
      sum += (double) count[i];
      sumSquared += (double) (count[i]*count[i]);
    }
    double exp = sum / (double) count.length;
    double expSq = sumSquared / (double) count.length;
    return Math.sqrt(expSq-exp*exp);
  }

  public static double getEntropy(long[] count) { // calculates entropy - 
    double[] p = new double[count.length];        // just for your edification...
    double total = 0;
    double entropy = 0;
    for(int i = 0; i < count.length; i++) {
      p[i] = (double) count[i];
      total += p[i];
    }
    for(int i = 0; i < count.length; i++) {
      p[i] /= total;
      entropy += p[i]*Math.log(p[i])/Math.log(2.0);
    }
    return -entropy;
  }
}
