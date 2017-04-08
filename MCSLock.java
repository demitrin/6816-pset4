import java.util.concurrent.atomic.*;

public class MCSLock implements Lock {
    /**
     * Add your fields here
     */
    private AtomicReference<QNode> tail = new AtomicReference<>();
    private ThreadLocal<QNode> myNode = new ThreadLocal<>();
    private ThreadLocal<QNode> pred = new ThreadLocal<>();

    public MCSLock() {
    }

    public void lock() {
        myNode.set(new QNode());
        pred.set(tail.getAndSet(myNode.get()));
        if (pred.get() != null) {
            myNode.get().isLocked.set(true);
            pred.get().next.set(myNode.get());
            while (myNode.get().isLocked.get()) {
            }
        }
    }

    public void unlock() {
        if (myNode.get().next.get() == null) {
            if (tail.compareAndSet(myNode.get(), null)) {
                return;
            }
            while(myNode.get().next.get() == null) {}
        }
        myNode.get().next.get().isLocked.set(false);
    }

    /**
     * Checks if the calling thread observes another thread concurrently
     * calling lock(), in the critical section, or calling unlock().
     *
     * @return true if another thread is present, else false
     */
    public boolean isContended() {
        return tail.get() != null;
    }

    class QNode {
        public volatile AtomicReference<QNode> next = new AtomicReference<QNode>();
        public volatile AtomicBoolean isLocked = new AtomicBoolean(false);
    }
}
