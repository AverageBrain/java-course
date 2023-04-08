package info.kgeorgiy.ja.morozov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet(ArrayView<T> data, Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    public ArraySet(List<T> data, Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(SortedSet<T> data, Comparator<? super T> comparator) {
        if (data.comparator().equals(comparator)) {
            this.data = List.copyOf(data);
            this.comparator = comparator;
        } else {
            TreeSet<T> tmpTreeSet = new TreeSet<>(comparator);
            tmpTreeSet.addAll(data);
            this.data = List.copyOf(tmpTreeSet);
            this.comparator = comparator;
        }
    }

    public ArraySet(Collection<? extends T> data, final Comparator<? super T> comparator) {
        TreeSet<T> tmpTreeSet = new TreeSet<>(comparator);
        tmpTreeSet.addAll(data);
        this.data = List.copyOf(tmpTreeSet);
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> data) {
        this(data, null);
    }

    private int binarySearch(T item) {
        return Collections.binarySearch(data, item, comparator);
    }

    private int lowerBound(T item, boolean inclusive) {
        int index = binarySearch(item); // -1 - pos, where pos - first greater element
        if (index < 0) {
            return -(index + 1) - 1;
        }
        return (inclusive ? index : index - 1);
    }

    private int upperBound(T item, boolean inclusive) {
        int index = binarySearch(item);
        if (index < 0) {
            return -(index + 1);
        } else {
            return (inclusive ? index : index + 1);
        }
    }

    @Override
    public T lower(T item) {
        int index = lowerBound(item, false);
        return index < 0 ? null : data.get(index);
    }

    @Override
    public T floor(T item) {
        int index = lowerBound(item, true);
        return index < 0 ? null : data.get(index);
    }

    @Override
    public T ceiling(T item) {
        int index = upperBound(item, true);
        return index == size() ? null : data.get(index);
    }

    @Override
    public T higher(T item) {
        int index = upperBound(item, false);
        return index == size() ? null : data.get(index);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ArrayView<T>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    private NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive,
                                   boolean exception) {
        int l = upperBound(fromElement, fromInclusive);
        int r = lowerBound(toElement, toInclusive);
        if (l > r) {
            if (!exception) return new ArraySet<>(Collections.emptyList(), comparator);
            throw new IllegalArgumentException("Left border more than right border");
        }
        return new ArraySet<>(data.subList(l, r + 1), comparator);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return subSet(fromElement, fromInclusive, toElement, toInclusive, true);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return data.isEmpty() ? this : subSet(first(), true, toElement, inclusive, false);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return data.isEmpty() ? this : subSet(fromElement, inclusive, last(), true, false);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return data.get(size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object item) {
        return (binarySearch((T) item) >= 0);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void dump() {
        for (T item : data) {
            System.out.println(item);
        }
    }
}
