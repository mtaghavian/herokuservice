package com.bcom.nsplacer.placement;

import java.util.*;

public class Fringe {

    private List<SearchState> list = new ArrayList<>();
    private boolean isQueue;
    private int index = 0;
    private boolean shuffle;

    public Fringe(boolean isQueue, boolean shuffle) {
        this.isQueue = isQueue;
        this.shuffle = shuffle;
    }

    public void put(List<SearchState> states) {
        for (int i = 0; i < states.size(); i++) {
            list.add(states.get(i));
        }
        if (isQueue) {
            if (shuffle) {
                Collections.shuffle(list.subList(index, list.size()));
            }
            Collections.sort(list.subList(index, list.size()));
        }

        //printLogs();
    }

    public void printLogs() {
        Map<Integer, Integer> depthCounter = new HashMap<>();
        for (int i = index; i < list.size(); i++) {
            int d = list.get(i).getDepth();
            if (!depthCounter.containsKey(d)) {
                depthCounter.put(d, 0);
            }
            depthCounter.put(d, depthCounter.get(d) + 1);
        }
        System.out.println("Fringe size: " + size());
        System.out.println("Fringe DepthCounter: " + depthCounter);
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
