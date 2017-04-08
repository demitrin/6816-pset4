class StopWatch {
  long startTime = 0; // nanoseconds
  long stopTime;      // nanoseconds
  /**
   * Starts the timer
   */
  void startTimer() {
    startTime = System.nanoTime();
  }
  /**
   * Stops the timer
   */
  void stopTimer() {
    stopTime = System.nanoTime();
  }
  /**
   * Reads the timer
   * @return
   *          number of milliseconds between last startTimer() to stopTimer() interval
   */
  double getElapsedTime() {
    return (stopTime - startTime) / 1000000.0;
  }  
}
