package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.NetworkNode;
import com.bcom.nsplacer.placement.SearchState;
import com.bcom.nsplacer.placement.VirtualLink;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.util.*;

public class HopCountRoutingAlgorithm extends RoutingAlgorithm {

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
        } else {
            List<RoutingPath> paths = new ArrayList<>();
            paths.add(new RoutingPath(findFirstFoundRoute(state, srcNode, dstNode, vl, timeout)));
            return paths;
        }
    }

    private List<NetworkLink> findFirstFoundRoute(SearchState state, String srcNode, String dstNode, VirtualLink vl, long timeout) {
        List<NetworkNode> list = new ArrayList<>();
        Set<String> bag = new HashSet<>();
        Map<String, NetworkNode> map = new HashMap<>();
        for (NetworkNode node : state.getNetworkGraph().getNodes()) {
            map.put(node.getLabel(), node);
        }
        list.add(map.get(srcNode));
        bag.add(srcNode);
        int expansionIndex = 0;
        List<Integer> returnIndexes = new ArrayList<>();
        returnIndexes.add(-1);
        long beginTime = System.currentTimeMillis();
        while (expansionIndex < list.size() && (System.currentTimeMillis() - beginTime) < timeout) {
            String label = list.get(expansionIndex).getLabel();
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth)
                        && link.getSrcNode().equals(label) && !link.isLoop()
                ) {
                    NetworkNode child = map.get(link.getDstNode());
                    if (!bag.contains(child.getLabel())) {
                        list.add(child);
                        bag.add(child.getLabel());
                        returnIndexes.add(expansionIndex);

                        if (child.getLabel().equals(dstNode)) {
                            List<NetworkLink> path = findPath(state, list, returnIndexes);
                            if (isConsiderLatency()) {
                                if (vl.getRequiredResourceValue(ResourceType.Latency) >= calcPathLatency(path)) {
                                    return path;
                                } else { // Forget this path and find another path
                                    list.remove(list.size() - 1);
                                    bag.remove(child.getLabel());
                                    returnIndexes.remove(returnIndexes.size() - 1);
                                }
                            } else {
                                return path;
                            }
                        }
                    }
                }
            }
            expansionIndex++;
        }
        return new ArrayList<>();
    }

    private int calcPathLatency(List<NetworkLink> path) {
        int sum = 0;
        for (NetworkLink l : path) {
            sum += l.getRemainingResourceValue(ResourceType.Latency);
        }
        return sum;
    }

    private List<NetworkLink> findPath(SearchState state, List<NetworkNode> list, List<Integer> returnIndexes) {
        List<NetworkLink> path = new ArrayList<>();
        int ri = returnIndexes.get(returnIndexes.size() - 1);
        NetworkNode linkDst = list.get(list.size() - 1);
        while (ri != -1) {
            NetworkNode linkSrc = list.get(ri);
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if (link.getSrcNode().equals(linkSrc.getLabel()) && link.getDstNode().equals(linkDst.getLabel())) {
                    path.add(link);
                    break;
                }
            }
            ri = returnIndexes.get(ri);
            linkDst = linkSrc;
        }
        Collections.reverse(path);
        return path;
    }
}
