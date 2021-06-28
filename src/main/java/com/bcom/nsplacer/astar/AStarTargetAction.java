package com.bcom.nsplacer.astar;

public interface AStarTargetAction {

    /**
     * Receives the target state
     * Return true if you want to terminate search, else, return false
     * @param target
     * @return
     */
    public boolean perform(AStarState target);
}
