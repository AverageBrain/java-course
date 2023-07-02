package info.kgeorgiy.ja.morozov.arrayset;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1539305624);
        list.add(1297757019);
        ArraySet<Integer> set = new ArraySet<>(list);
        ArraySet<Integer> x = (ArraySet<Integer>) set.subSet(-1621015911, false, -1429233748, false);
        x.dump();
    }
}
