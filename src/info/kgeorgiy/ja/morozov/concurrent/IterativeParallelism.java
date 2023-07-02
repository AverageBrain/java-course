package info.kgeorgiy.ja.morozov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Constructs an implementation of {@link AdvancedIP} without {@link ParallelMapper}
     * */
    public IterativeParallelism() {
        parallelMapper = null;
    }


    /**
     * Constructs an implementation of {@link AdvancedIP} with {@link ParallelMapper}
     *
     * @param parallelMapper {@link ParallelMapper} which performs functions
     * */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <R, T> R parallelRun(int threads, List<T> values,
                                Function<Stream<T>, R> function,
                                Function<Stream<R>, R> reduceFunction) throws InterruptedException {
        int threadsCount = Math.min(threads, values.size());
        int blockSize = values.size() / threadsCount;
        int remainder = values.size() % threadsCount;
        List<Stream<T>> groups = new ArrayList<>();
        for (int left = 0; left < values.size(); ) {
            int right = left + blockSize + (remainder-- > 0 ? 1 : 0);
            groups.add(values.subList(left, right).stream());
            left = right;
        }

        if (parallelMapper == null) {
            List<R> blockResults = new ArrayList<>(Collections.nCopies(threadsCount, null));
            List<Thread> threadsList = new ArrayList<>();
            for (int i = 0; i < threadsCount; i++) {
                int finalI = i;
                threadsList.add(new Thread(() -> blockResults.set(finalI, function.apply(groups.get(finalI)))));
                threadsList.get(finalI).start();
            }

            InterruptedException interruptedException = null;
            for (int i = 0; i < threadsCount; i++) {
                try {
                    threadsList.get(i).join();
                } catch (InterruptedException e) {
                    if (interruptedException == null) {
                        interruptedException = e;
                    } else {
                        interruptedException.addSuppressed(e);
                    }
                    threadsList.get(i).interrupt();
                }
            }
            if (interruptedException != null) {
                throw interruptedException;
            }
            return reduceFunction.apply(blockResults.stream());
        } else {
            final List<R> results = parallelMapper.map(function, groups);
            return reduceFunction.apply(results.stream());
        }
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelRun(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelRun(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelRun(threads, values,
                (stream -> stream.max(comparator).orElse(null)),
                (stream -> stream.max(comparator).orElse(null)));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(item -> item));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(threads, values,
                stream -> (int) stream.filter(predicate).count(),
                stream -> stream.reduce(0, Integer::sum));
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return parallelRun(threads, values,
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return reduce(threads, map(threads, values, lift), monoid);
    }
}
