package com.bcom.nsplacer.astar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AStarFringe {

    private List<AStarState> list = new ArrayList<>();
    private int index = 0;

    public AStarFringe() {
    }

    public void put(List<AStarState> states) {
        for (int i = 0; i < states.size(); i++) {
            list.add(states.get(i));
        }
        Collections.sort(list.subList(index, list.size()));
    }

    public boolean isEmpty() {
        return index == list.size();
    }

    public int size() {
        return list.size() - index;
    }

    public AStarState take() {
        AStarState state = list.get(index);
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
    }

}
