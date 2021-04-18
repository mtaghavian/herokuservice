package com.bcom.nsplacer.placement;

import java.util.List;

public interface RoutingAlgorithm {

    public List<NetworkLink> route(SearchState state, NetworkNode srcNode, NetworkNode dstNode, VirtualLink vl);
}
