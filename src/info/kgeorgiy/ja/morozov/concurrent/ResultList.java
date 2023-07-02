package info.kgeorgiy.ja.morozov.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Thread-safe list for result
 *
 * @author Anton Morozov
 * */
public class ResultList<R> {
    private final List<R> list;
    private RuntimeException exceptions;
    private int finished;

    /**
     * Create immutable list consisting of n copies
     *
     * @param size - number of element
     * */
    public ResultList(final int size) {
        list = new ArrayList<>(Collections.nCopies(size, null));
        finished = 0;
        exceptions = null;
    }


    /**
     * Replaces the element at the specified position
     *
     * @param index - specified position
     * @param value - element, which replaced
     * */
    public synchronized void set(final int index, final R value) {
        list.set(index, value);
        finished++;
        if (finished == list.size()) {
            notify();
        }
    }


    /**
     * Waits all values and then returns list
     *
     * @throws InterruptedException if an error occurred during to wait
     * */
    public synchronized List<R> getList() throws InterruptedException {
        while (finished != list.size()) {
            wait();
        }
        if (exceptions != null) {
            throw exceptions;
        }
        return list;
    }


    /**
     * Set exceptions, which occurred during to process elements
     *
     * @param exp exception, which added
     * */
    public synchronized void setException(final RuntimeException exp) {
        if (exp == null) {
            exceptions = exp;
        } else {
            exceptions.addSuppressed(exp);
        }
    }
}