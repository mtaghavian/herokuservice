package com.bcom.nsplacer.misc;

import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.NetworkNode;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphUtils {

    public static Map<String, Integer> getBetweenness(NetworkGraph graph) {
        Map<String, Integer> btness = new HashMap<>();
        Map<String, Map<String, String>> linkMap = new HashMap<>();
        for (NetworkLink link : graph.getLinks()) {
            if (!link.isLoop()) {
                btness.put(link.getLabel(), 0);
                Map<String, String> map = linkMap.get(link.getSrcNode());
                if (map == null) {
                    map = new HashMap<>();
                }
                map.put(link.getDstNode(), link.getLabel());
                linkMap.put(link.getSrcNode(), map);
            }
        }
        FWRouting fWarshallAlgorithm = new FWRouting();
        fWarshallAlgorithm.build(graph);
        for (NetworkNode n1 : graph.getNodes()) {
            for (NetworkNode n2 : graph.getNodes()) {
                if (n1.getLabel().equals(n2.getLabel())) {
                    continue;
                }
                List<String> path = fWarshallAlgorithm.getPath(n1.getLabel(), n2.getLabel());
                for (int i = 0; i < path.size() - 1; i++) {
                    String linkLabel = linkMap.get(path.get(i)).get(path.get(i + 1));
                    btness.put(linkLabel, btness.get(linkLabel) + 1);
                }
            }
        }
        return btness;
    }

    public static Integer maximumRemainingBandwidth(List<NetworkLink> links) {
        int d = 0;
        for (NetworkLink link : links) {
            d = Math.max(d, link.getRemainingResourceValue(ResourceType.Bandwidth));
        }
        return d;
    }

    public static Integer minimumRemainingBandwidth(List<NetworkLink> links) {
        int d = Integer.MAX_VALUE;
        for (NetworkLink link : links) {
            d = Math.min(d, link.getRemainingResourceValue(ResourceType.Bandwidth));
        }
        return d;
    }

    public static Double averageRemainingBandwidth(List<NetworkLink> links) {
        double d = 0.0;
        for (NetworkLink link : links) {
            d += link.getRemainingResourceValue(ResourceType.Bandwidth);
        }
        d /= links.size();
        return d;
    }

}
