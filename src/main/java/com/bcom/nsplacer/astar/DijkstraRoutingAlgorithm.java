package com.bcom.nsplacer.astar;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.RoutingPath;

import java.util.*;

public class DijkstraRoutingAlgorithm extends RoutingAlgorithm {

    private Map<String, Map<String, String>> linkMap;

    public DijkstraRoutingAlgorithm(NetworkGraph graph) {
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
        Map<String, Long> distMap = new HashMap<>();
        Map<String, String> prevMap = new HashMap<>();
        List<String> queue = new ArrayList<>();
        for (NetworkNode node : state.getNetworkGraph().getNodes()) {
            distMap.put(node.getLabel(), (long) Integer.MAX_VALUE);
            queue.add(node.getLabel());
        }
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -Long.compare(distMap.get(o1), distMap.get(o2));
            }
        };
        distMap.put(srcNode, 0l);
        while (!queue.isEmpty()) {
            Collections.sort(queue, comp);
            String node = queue.remove(queue.size() - 1);
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if (link.getSrcNode().equals(node) && !link.isLoop() &&
                        (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))) {
                    long alt = distMap.get(node) + 1l;
                    if (alt < distMap.get(link.getDstNode())) {
                        distMap.put(link.getDstNode(), alt);
                        prevMap.put(link.getDstNode(), node);
                    }
                }
            }
        }
        List<NetworkLink> links = new ArrayList<>();
        String dst = dstNode;
        while (true) {
            String src = prevMap.get(dst);
            if (src == null) {
                return new ArrayList<>();
            }
            String linkLabel = linkMap.get(src).get(dst);
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if (link.getLabel().equals(linkLabel)) {
                    links.add(link);
                    break;
                }
            }
            if (src.equals(srcNode)) {
                break;
            } else {
                dst = src;
            }
        }
        Collections.reverse(links);
        List<RoutingPath> paths = new ArrayList<>();
        paths.add(new RoutingPath(links));
        return paths;
    }
}
