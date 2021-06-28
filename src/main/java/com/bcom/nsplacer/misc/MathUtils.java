package com.bcom.nsplacer.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MathUtils {

    public static <E extends Comparable<E>> List<E> quartile(List<E> values) {
        List<E> arr = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            arr.add(values.get(i));
        }
        Collections.sort(arr);
        List<E> list = new ArrayList<>();
        if (!values.isEmpty()) {
            list.add(arr.get(0));
            list.add(arr.get((int) (arr.size() * 0.25)));
            list.add(arr.get((int) (arr.size() * 0.50)));
            list.add(arr.get((int) (arr.size() * 0.75)));
            list.add(arr.get(arr.size() - 1));
        }
        return list;
    }

    public static <E> double genericAverage(List<E> values) {
        double sum = 0;
        for (int i = 0; i < values.size(); i++) {
            sum += Double.parseDouble("" + values.get(i));
        }
        return sum / values.size();
    }

    public static double average(List<Double> values) {
        return sum(values) / values.size();
    }

    public static double sum(List<Double> values) {
        double sum = 0;
        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum;
    }

}
