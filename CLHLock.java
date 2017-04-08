import java.util.concurrent.atomic.*;

public class CLHLock implements Lock {
    /**
     * Add your fields here
     */
    private AtomicReference<QNode> tail = new AtomicReference<>(new QNode());
    private ThreadLocal<QNode> myNode = new ThreadLocal<>();
    private ThreadLocal<QNode> pred = new ThreadLocal<>();

    public CLHLock() {
        tail.get().release();
    }

    public void lock() {
        myNode.set(new QNode());
        pred.set(tail.getAndSet(myNode.get()));
        while (pred.get().isLocked()) {
        }

    }

    public void unlock() {
        myNode.get().release();
    }

    /**
     * Checks if the calling thread observes another thread concurrently
     * calling lock(), in the critical section, or calling unlock().
     *
     * @return true if another thread is present, else false
     */
    public boolean isContended() {
        return tail.get().isLocked();
    }

    class QNode {
        /**
         * Add your fields here
         */

        private AtomicBoolean isLocked = new AtomicBoolean(true);

        public boolean isLocked() {
            return isLocked.get();
        }

        public void release() {
            isLocked.set(false);
        }
    }
}
