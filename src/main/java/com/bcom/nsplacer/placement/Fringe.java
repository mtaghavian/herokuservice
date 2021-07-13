package com.bcom.nsplacer.placement;

import java.util.*;

public class Fringe {

    private List<SearchState> list = new ArrayList<>();
    private boolean isQueue;
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
                Collections.shuffle(list);
            }
            Collections.sort(list);
        }
        //printLogs();
    }

    public void printLogs() {
        Map<Integer, Integer> depthCounter = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
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
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public SearchState take() {
        if (isEmpty()) {
            return null;
        }
        return list.remove(list.size() - 1);
    }

}
