package info.kgeorgiy.ja.morozov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Implementation {@link ParallelMapper} interfaces.
 * Applies map to an argument list
 *
 * @author Anton Morozov
 * */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final SyncQueue<Runnable> queue;
    private volatile boolean closed;


    /**
     * Constructor create {@code threads} Threads for solving tasks
     *
     * @param threads number of threads
     * */
    public ParallelMapperImpl(final int threads) {
        this.queue = new SyncQueue<>();
        Runnable runnable = () -> {
            try {
                while (!Thread.interrupted()) {
                    queue.poll().run();
                }
            } catch (final InterruptedException ignored) {
                // ignored
            }
        };
        this.threads = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final Thread thread = new Thread(runnable);
            thread.start();
            this.threads.add(thread);
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final ResultList<R> resultList = new ResultList<>(args.size());
        if (!closed) {
            for (int i = 0; i < args.size(); i++) {
                final int finalI = i;
                queue.add(() -> {
                    R resultValue = null;
                    if (!closed) {
                        try {
                            resultValue = f.apply(args.get(finalI));
                        } catch (final RuntimeException e) {
                            resultList.setException(e);
                        }
                    }
                    resultList.set(finalI, resultValue);
                });
            }
        }
        return resultList.getList();
    }


    @Override
    public void close() {
        if (!closed) {
            closed = true;
            for (Thread t : threads) {
                t.interrupt();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (final InterruptedException ignored) {

                }
            }
        }
    }
}
