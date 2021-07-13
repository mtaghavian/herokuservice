package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.util.*;

public class DijkstraRoutingAlgorithm extends RoutingAlgorithm {

    private Map<String, Map<String, String>> linkNodeMap;
    private Map<String, NetworkLink> linkMap = new HashMap<>();
    private List<RoutingPath> foundPaths;

    public DijkstraRoutingAlgorithm(NetworkGraph graph) {
        linkNodeMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                Map<String, String> map = linkNodeMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkNodeMap.put(link.getSrcNode(), map);
            }
        }
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, ServiceLink vl, long timeout) {
        if (srcNode.equals(dstNode)) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getCurrentResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        && link.isLoop() && link.getSrcNode().equals(srcNode)) {
                    List<RoutingPath> paths = new ArrayList<>();
                    paths.add(new RoutingPath(CollectionUtils.concat(new ArrayList<>(), link)));
                    return paths;
                }
            }
            return new ArrayList<>();
        }
        for (NetworkLink link : state.getNetworkGraph().getLinks()) {
            linkMap.put(link.getLabel(), link);
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
            String nodeLabel = queue.remove(queue.size() - 1);
            if (linkNodeMap.get(nodeLabel) == null) {
                // We have an unconnected graph
                continue;
            }
            for (String dstNodeLabel : linkNodeMap.get(nodeLabel).keySet()) {
                NetworkLink link = linkMap.get(linkNodeMap.get(nodeLabel).get(dstNodeLabel));
                if (link.getCurrentResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth)) {
                    long alt = distMap.get(nodeLabel) + (long) link.getCurrentResourceValue(ResourceType.Latency);
                    if (alt < distMap.get(link.getDstNode())) {
                        distMap.put(link.getDstNode(), alt);
                        prevMap.put(link.getDstNode(), nodeLabel);
                    }
                }
            }
        }
        if (prevMap.get(dstNode) == null) {
            return new ArrayList<>();
        }
        foundPaths = new ArrayList<>();
        foundPaths.add(new RoutingPath(getPath(prevMap, srcNode, dstNode)));
        return foundPaths;
    }

    private List<NetworkLink> getPath(Map<String, String> prevMap, String srcNode, String dstNode) {
        List<String> list = new ArrayList<>();
        list.add(dstNode);
        while (!dstNode.equals(srcNode)) {
            list.add(prevMap.get(dstNode));
            dstNode = prevMap.get(dstNode);
        }
        Collections.reverse(list);
        List<NetworkLink> path = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            NetworkLink networkLink = linkMap.get((linkNodeMap.get(list.get(i))).get(list.get(i + 1)));
            path.add(networkLink);
        }
        return path;
    }
}
