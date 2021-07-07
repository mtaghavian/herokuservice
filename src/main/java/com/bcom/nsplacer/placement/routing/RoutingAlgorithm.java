package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.placement.SearchState;
import com.bcom.nsplacer.placement.VirtualLink;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class RoutingAlgorithm {

    private int maxNumPaths = 1;
    private boolean considerLatency = false;

    public abstract List<RoutingPath> route(SearchState state, String srcNode, String dstNode, VirtualLink vl, long timeout);
}
