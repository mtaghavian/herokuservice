package com.bcom.nsplacer.astar;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public abstract class AStarState implements Comparable<AStarState> {

    private int fCost;
    private AStarState parent;

    @Override
    public int compareTo(AStarState o) {
        return fCost - o.fCost;
    }

    public List<AStarState> getPath() {
        List<AStarState> path = new ArrayList<>();
        AStarState state = this;
        while (state.getParent() != null) {
            path.add(state);
            state = state.getParent();
        }
        path.add(state);
        Collections.reverse(path);
        return path;
    }

    public abstract boolean isTarget();

    public abstract int gCost();

    public abstract int hCost();

    public abstract List<AStarState> expand();
}
