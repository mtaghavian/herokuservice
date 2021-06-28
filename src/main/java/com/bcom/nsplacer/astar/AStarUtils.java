package com.bcom.nsplacer.astar;

import java.util.Arrays;
import java.util.List;

public class AStarUtils {

    public static void search(AStarState root, long timeout, AStarTargetAction action) {
        AStarFringe fringe = new AStarFringe();
        fringe.put(Arrays.asList(root));
        long time = System.currentTimeMillis();
        while (!fringe.isEmpty() && ((System.currentTimeMillis() - time) < timeout)) {
            AStarState state = fringe.take();
            if (state.isTarget()) {
                boolean finish = action.perform(state);
                if (finish) {
                    return;
                }
            } else {
                List<AStarState> children = state.expand();
                for (AStarState s : children) {
                    s.setParent(state);
                    s.setFCost(s.gCost() + s.hCost());
                }
                fringe.put(children);
            }
        }
    }

}
