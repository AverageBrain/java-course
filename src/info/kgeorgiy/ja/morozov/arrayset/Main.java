package info.kgeorgiy.ja.morozov.arrayset;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(6);
        list.add(4);
        ArraySet<Integer> set = new ArraySet<>(list);
        NavigableSet<Integer> x = set.descendingSet();
        NavigableSet<Integer> y = x.descendingSet();
        Iterator<Integer> iter = set.iterator();
        if (iter.hasNext()) {
            Integer i = iter.next();
        }
    }
}
