package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.InitializerParameters;
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

    private List<ServiceNode> vnfs = new ArrayList<>();

    private List<ServiceLink> virtualLinks = new ArrayList<>();

    private int latency = 0;

    public List<ServiceNode> traverse() {
        List<ServiceNode> bfsList = new ArrayList<>();
        Set<String> bfsBag = new HashSet<>();
        Map<String, ServiceNode> map = new HashMap<>();
        for (int i = 0; i < vnfs.size(); i++) {
            map.put(vnfs.get(i).getLabel(), vnfs.get(i));
        }
        bfsList.add(map.get(dataFlowSrcVNF));
        bfsBag.add(dataFlowSrcVNF);
        int i = 0;
        while (i < bfsList.size()) {
            String label = bfsList.get(i).getLabel();
            for (ServiceLink l : virtualLinks) {
                if (l.getSrcVNF().equals(label)) {
                    ServiceNode child = map.get(l.getDstVNF());
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
        for (ServiceNode node : getVnfs()) {
            sumCpu += node.getRequiredResourceValue(ResourceType.Cpu);
            sumStorage += node.getRequiredResourceValue(ResourceType.Storage);
        }
        for (ServiceLink link : getVirtualLinks()) {
            sumBandwidth += link.getRequiredResourceValue(ResourceType.Bandwidth);
        }
        return "Total remaining resources (cpu, storage, bandwidth) = (" + sumCpu + ", " + sumStorage + ", " + sumBandwidth + ")";
    }

    public void create(TopologyType topologyType, int serviceSize, InitializerParameters cpu, InitializerParameters storage, InitializerParameters bandwidth, InitializerParameters latency) {
        vnfs.clear();
        virtualLinks.clear();
        setDataFlowSrcVNF("V1");
        for (int i = 0; i < serviceSize; i++) {
            ServiceNode v = new ServiceNode();
            v.setLabel("V" + (i + 1));
            v.setRequiredResourceValue(ResourceType.Cpu, cpu.get());
            v.setRequiredResourceValue(ResourceType.Storage, storage.get());
            getVnfs().add(v);
        }
        if (TopologyType.DaisyChain.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i < serviceSize; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + i);
                l.setDstVNF("V" + (i + 1));
                l.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                l.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                clone.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                clone.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Ring.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i <= serviceSize; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + i);
                l.setDstVNF("V" + ((i == serviceSize) ? 1 : (i + 1)));
                l.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                l.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                clone.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                clone.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        } else if (TopologyType.Star.equals(topologyType)) {
            int linkIndex = 1;
            for (int i = 1; i < serviceSize; i++) {
                ServiceLink l = new ServiceLink();
                l.setLabel("VL" + linkIndex);
                l.setSrcVNF("V" + 1);
                l.setDstVNF("V" + (i + 1));
                l.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                l.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(l);
                linkIndex++;

                ServiceLink clone = l.clone();
                clone.setLabel("VL" + linkIndex);
                clone.setSrcVNF(l.getDstVNF());
                clone.setDstVNF(l.getSrcVNF());
                clone.setRequiredResourceValue(ResourceType.Bandwidth, bandwidth.get());
                clone.setRequiredResourceValue(ResourceType.Latency, latency.get());
                getVirtualLinks().add(clone);
                linkIndex++;
            }
        }
    }
}
