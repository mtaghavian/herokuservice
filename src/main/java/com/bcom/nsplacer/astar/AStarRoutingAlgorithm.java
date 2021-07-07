package com.bcom.nsplacer.astar;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.FWRouting;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.SearchState;
import com.bcom.nsplacer.placement.VirtualLink;
import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.RoutingPath;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AStarRoutingAlgorithm extends RoutingAlgorithm {

    private FWRouting fwRouting;
    private Map<String, Map<String, String>> linkMap;

    public AStarRoutingAlgorithm(NetworkGraph graph) {
        fwRouting = new FWRouting();
        fwRouting.build(graph);

        linkMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                Map<String, String> map = linkMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkMap.put(link.getSrcNode(), map);
            }
        }
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, VirtualLink vl, long timeout) {
        if (srcNode.equals(dstNode)) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        && link.isLoop() && link.getSrcNode().equals(srcNode)) {
                    List<RoutingPath> paths = new ArrayList<>();
                    paths.add(new RoutingPath(CollectionUtils.concat(new ArrayList<>(), link)));
                    return paths;
                }
            }
            return new ArrayList<>();
        }
        List<RoutingPath> paths = new ArrayList<>();
        AStarUtils.search(new NetworkNodeState(srcNode, dstNode, fwRouting, vl), timeout, new AStarTargetAction() {
            @Override
            public boolean perform(AStarState target) {
                RoutingPath routingPath = new RoutingPath();
                List<AStarState> path = target.getPath();
                for (int i = 0; i < path.size() - 1; i++) {
                    String linkLabel = linkMap.get(((NetworkNodeState) path.get(i)).getCurrentNode())
                            .get(((NetworkNodeState) path.get(i + 1)).getCurrentNode());
                    for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                        if (link.getLabel().equals(linkLabel)) {
                            routingPath.getLinks().add(link);
                            break;
                        }
                    }
                }
                paths.add(routingPath);
                return true;
            }
        });
        return paths;
    }

    @Getter
    @Setter
    private class NetworkNodeState extends AStarState {

        private String currentNode, targetNode;
        private FWRouting fwAlgorithm;
        private VirtualLink vl;

        public NetworkNodeState(String currentNode, String targetNode, FWRouting fwAlgorithm, VirtualLink vl) {
            this.currentNode = currentNode;
            this.targetNode = targetNode;
            this.fwAlgorithm = fwAlgorithm;
            this.vl = vl;
        }

        @Override
        public boolean isTarget() {
            return currentNode.equals(targetNode);
        }

        @Override
        public int gCost() {
            return getPath().size() - 1;
        }

        @Override
        public int hCost() {
            return fwAlgorithm.getDist(currentNode, targetNode);
        }

        @Override
        public List<AStarState> expand() {
            List<AStarState> list = new ArrayList<>();
            for (NetworkLink link : fwAlgorithm.getGraph().getLinks()) {
                if (link.getSrcNode().equals(currentNode) && !link.isLoop()
                        && (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        // && (!assertLatency || checkPathLatency(path, link, vl))
                        && !hasLoop(link.getDstNode())
                ) {
                    list.add(new NetworkNodeState(link.getDstNode(), targetNode, fwAlgorithm, vl));
                }
            }
            return list;
        }

        private boolean hasLoop(String dst) {
            for (AStarState s : getPath()) {
                if (((NetworkNodeState) s).getCurrentNode().equals(dst)) {
                    return true;
                }
            }
            return false;
        }
    }
}
