package info.kgeorgiy.ja.morozov.concurrent;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Thread-safe queue
 *
 * @author Anton Morozov
 * */
public class SyncQueue<T> {
    private final Queue<T> queue;

    /**
     * Create empty queue
     *
     * @see LinkedList
     * */
    SyncQueue() {
        queue = new LinkedList<>();
    }


    /**
     * Add value to value and not
     *
     * @param value - insert the specified element
     * */
    public synchronized void add(final T value) {
        queue.add(value);
        notify();
    }


    /**
     * Retrieves and removes the head of this queue.
     *
     * @throws InterruptedException if an error occurred during to wait
     * */
    public synchronized T poll() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.poll();
    }
}