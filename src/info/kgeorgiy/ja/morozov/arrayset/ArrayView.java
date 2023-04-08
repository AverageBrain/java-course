package info.kgeorgiy.ja.morozov.arrayset;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public class ArrayView<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> data;
    private final boolean reversed;

    public ArrayView(List<T> data) {
        if (data instanceof ArrayView<T> arrayView) {
            this.reversed = !arrayView.reversed;
            this.data = arrayView.data;
        } else {
            this.reversed = true;
            this.data = data;
        }
    }

    @Override
    public T get(int i) {
        return (reversed ? data.get(size() - i - 1) : data.get(i));
    }

    @Override
    public int size() {
        return data.size();
    }
}
