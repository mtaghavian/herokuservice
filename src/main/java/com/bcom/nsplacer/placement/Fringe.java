package com.bcom.nsplacer.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Fringe {

    private List<SearchState> list = new ArrayList<>();
    private boolean isQueue;
    private int index = 0;

    public Fringe(boolean isQueue) {
        this.isQueue = isQueue;
    }

    public void put(List<SearchState> states) {
        for (int i = 0; i < states.size(); i++) {
            list.add(states.get(i));
        }
        if (isQueue) {
            Collections.sort(list.subList(index, list.size()));
        }
    }

    public boolean isEmpty() {
        if (isQueue) {
            return index == list.size();
        } else {
            return list.isEmpty();
        }
    }

    public int size() {
        if (isQueue) {
            return list.size() - index;
        } else {
            return list.size();
        }
    }

    public SearchState take() {
        if (isEmpty()) {
            return null;
        }
        if (isQueue) {
            SearchState state = list.get(index);
            index++;

            int maxSize = 5000;
            while (index > maxSize) {
                for (int i = maxSize; i < list.size(); i++) {
                    list.set(i - maxSize, list.get(i));
                }
                for (int i = 0; i < maxSize; i++) {
                    list.remove(list.size() - 1);
                }
                index -= maxSize;
            }
            return state;
        } else {
            return list.remove(list.size() - 1);
        }
    }

}
