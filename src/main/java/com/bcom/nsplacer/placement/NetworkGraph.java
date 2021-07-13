package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class NetworkGraph {

    private List<NetworkNode> nodes = new ArrayList<>();

    private List<NetworkLink> links = new ArrayList<>();

    public NetworkGraph clone() {
        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < nodes.size(); i++) {
            graph.getNodes().add(nodes.get(i).clone());
        }
        for (int i = 0; i < links.size(); i++) {
            graph.getLinks().add(links.get(i).clone());
        }
        return graph;
    }

    public int getTotalRemainingResourceValue(boolean nodeResource, ResourceType type) {
        int sum = 0;
        if (nodeResource) {
            for (NetworkNode node : getNodes()) {
                sum += node.getCurrentResourceValue(type);
            }
        } else {
            for (NetworkLink link : getLinks()) {
                if (!link.isLoop()) {
                    sum += link.getCurrentResourceValue(type);
                }
            }
        }
        return sum;
    }

    public int getTotalMaximumResourceValue(boolean nodeResource, ResourceType type) {
        int sum = 0;
        if (nodeResource) {
            for (NetworkNode node : getNodes()) {
                sum += node.getMaximumResourceValue(type);
            }
        } else {
            for (NetworkLink link : getLinks()) {
                if (!link.isLoop()) {
                    sum += link.getMaximumResourceValue(type);
                }
            }
        }
        return sum;
    }

    public int getTotalUsedResourceValue(boolean nodeResource, ResourceType type) {
        return getTotalMaximumResourceValue(nodeResource, type) - getTotalRemainingResourceValue(nodeResource, type);
    }

    public List<Integer> listRemainingBandwidth() {
        Collections.sort(getLinks(), Comparator.comparingInt(o -> o.getCurrentResourceValue(ResourceType.Bandwidth)));
        List<Integer> list = new ArrayList<>();
        for (NetworkLink l : getLinks()) {
            if (!l.isLoop()) {
                list.add(l.getCurrentResourceValue(ResourceType.Bandwidth));
            }
        }
        return list;
    }

    public Map<String, Integer> mapOfAggregatedRemainingBandwidth() {
        Map<String, Integer> map = new HashMap<>();
        for (NetworkLink l : getLinks()) {
            if (!l.isLoop()) {
                int rb = l.getCurrentResourceValue(ResourceType.Bandwidth);
                Integer nb = map.get(l.getSrcNode());
                map.put(l.getSrcNode(), (nb == null) ? rb : nb + rb);
            }
        }
        return map;
    }

    public List<Integer> listOfAggregatedRemainingBandwidth() {
        Map<String, Integer> map = mapOfAggregatedRemainingBandwidth();
        List<Integer> list = new ArrayList<>();
        for (Object k : map.keySet()) {
            list.add(map.get(k));
        }
        Collections.sort(list);
        return list;
    }

    public void sortNodes() {
        Collections.sort(getNodes(), (o1, o2) -> {
            double sum = 0;
            for (Resource r : o1.getCurrentResources()) {
                sum += ((double) o1.getCurrentResourceValue(r.getType()) / o1.getMaximumResourceValue(r.getType())) -
                        ((double) o2.getCurrentResourceValue(r.getType()) / o2.getMaximumResourceValue(r.getType()));
            }
            return (int) Math.round(Math.signum(sum));
        });
    }

    public String getStatus() {
        int sumCpu = 0, sumStorage = 0, sumBandwidth = 0;
        for (NetworkNode node : getNodes()) {
            sumCpu += node.getCurrentResourceValue(ResourceType.Cpu);
            sumStorage += node.getCurrentResourceValue(ResourceType.Storage);
        }
        for (NetworkLink link : getLinks()) {
            if (!link.isLoop()) {
                sumBandwidth += link.getCurrentResourceValue(ResourceType.Bandwidth);
            }
        }
        return "Total remaining resources (cpu, storage, bandwidth) = (" + sumCpu + ", " + sumStorage + ", " + sumBandwidth + ")";
    }

}
