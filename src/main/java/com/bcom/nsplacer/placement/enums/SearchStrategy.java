package com.bcom.nsplacer.placement.enums;

public enum SearchStrategy {

    VOTE, // A prioritized voting strategy over some other strategies
    ABO,
    DBO,
    EDFF, // Decreasing First Fit
    EIFF, // Increasing First Fit
    DFF,
    IFF,
    SAP,
    Random;

    public boolean isMaximizer() {
        if (this.equals(SAP)) {
            return true;
        } else {
            return false;
        }
    }
}
