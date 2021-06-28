package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.enums.TopologyType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class ServiceGraph {

    private String dataFlowSrcVNF;

    private List<VNF> vnfs = new ArrayList<>();

    private List<VirtualLink> virtualLinks = new ArrayList<>();

    private int latency = 0;

    public List<VNF> traverse() {
        List<VNF> bfsList = new ArrayList<>();
        Set<String> bfsBag = new HashSet<>();
        Map<String, VNF> map = new HashMap<>();
        for (int i = 0; i < vnfs.size(); i++) {
            map.put(vnfs.get(i).getLabel(), vnfs.get(i));
        }
        bfsList.add(map.get(dataFlowSrcVNF));
        bfsBag.add(dataFlowSrcVNF);
        int i = 0;
        while (i < bfsList.size()) {
            String label = bfsList.get(i).getLabel();
            for (VirtualLink l : virtualLinks) {
                if (l.getSrcVNF().equals(label)) {
                    VNF child = map.get(l.getDstVNF());
                    if (!bfsBag.contains(child.getLabel())) {
                        bfsList.add(child);
                        bfsBag.add(child.getLabel());
                    }
                }
            }
            i++;
        }
        return bfsList;
    }

    public ServiceGraph clone() {
        ServiceGraph graph = new ServiceGraph();
        graph.dataFlowSrcVNF = dataFlowSrcVNF;
        for (int i = 0; i < vnfs.size(); i++) {
            graph.getVnfs().add(vnfs.get(i).clone());
        }
        for (int i = 0; i < virtualLinks.size(); i++) {
            graph.getVirtualLinks().add(virtualLinks.get(i).clone());
        }
        return graph;
    }

    public String getStatus() {
        int sumCpu = 0, sumStorage = 0, sumBandwidth = 0;
        for (VNF node : getVnfs()) {
            sumCpu += node.getRequiredResourceValue(ResourceType.Cpu);
            sumStorage += node.getRequiredResourceValue(ResourceType.Storage);
        }
        for (VirtualLink link : getVirtualLinks()) {
            sumBandwidth += link.getRequiredResourceValue(ResourceType.Bandwidth);
        }
        return "Total remaining resources (cpu, storage, bandwidth) = (" + sumCpu + ", " + sumStorage + ", " + sumBandwidth + ")";
    }

    public void create(Random random, TopologyType topologyType, int serviceSize, int maxCpuDemand, int maxStorageDemand, int maxBandwidthDemand, int maxLatencyDemand) {
        vnfs.clear();
        virtualLinks.clear();
        setDataFlowSrcVNF("V1");
        for (int i = 0; i < serviceSize; i++) {
            VNF v = new VNF();
            v.setLabel("V" + (i + 1));
            if (random != null) {
                v.setRandomValues(random, maxCpuDemand, maxStorageDemand);
            } else {
                v.setRequiredResourceValue(ResourceType.Cpu, maxCpuDemand);
                v.setRequiredResourceValue(ResourceType.Storage, maxStorageDemand);
            }
            getVnfs().add(v);
        }
        if (TopologyType.DaisyChain.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i < serviceSize; i++) {
                VirtualLink l = new VirtualLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + i);
                l.setDstVNF("V" + (i + 1));
                if (random != null) {
                    l.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    l.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    l.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(l);
                linkIndex++;

                VirtualLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                if (random != null) {
                    clone.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    clone.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    clone.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Ring.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i <= serviceSize; i++) {
                VirtualLink l = new VirtualLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + i);
                l.setDstVNF("V" + ((i == serviceSize) ? 1 : (i + 1)));
                if (random != null) {
                    l.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    l.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    l.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(l);
                linkIndex++;

                VirtualLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                if (random != null) {
                    clone.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    clone.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    clone.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Star.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i < serviceSize; i++) {
                VirtualLink l = new VirtualLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + 1);
                l.setDstVNF("V" + (i + 1));
                if (random != null) {
                    l.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    l.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    l.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(l);
                linkIndex++;

                VirtualLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                if (random != null) {
                    clone.setRandomValues(random, maxBandwidthDemand, maxLatencyDemand);
                } else {
                    clone.setRequiredResourceValue(ResourceType.Bandwidth, maxBandwidthDemand);
                    clone.setRequiredResourceValue(ResourceType.Latency, maxLatencyDemand);
                }
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        }
    }
}
