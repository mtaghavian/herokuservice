package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;

import java.io.IOException;
import java.util.Random;

public class ZooTopologyImportExportManager {

    public static NetworkGraph importFromXML(Random random, String xml, int cpu, int storage, int bandwidth, boolean randomLatency, int latencyRange, int latencyOffset) throws IOException {
        if (!xml.contains("<graphml ")) {
            throw new IOException("Could not find an XML of Zoo Topology!");
        }
        NetworkGraph ng = new NetworkGraph();
        int pointer = 0;
        int linkIndex = 1;
        while (true) {
            pointer = xml.indexOf("<node ", pointer + 1);
            if (pointer < 0) {
                break;
            }
            int p = xml.indexOf("id=\"", pointer);
            String label = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            NetworkNode node = new NetworkNode();
            node.setMaximumResourceValue(ResourceType.Cpu, cpu);
            node.setRemainingResourceValue(ResourceType.Cpu, cpu);
            node.setMaximumResourceValue(ResourceType.Storage, storage);
            node.setRemainingResourceValue(ResourceType.Storage, storage);
            node.setLabel(label);
            ng.getNodes().add(node);

            NetworkLink link = new NetworkLink();
            link.setLabel("L" + linkIndex);
            link.setSrcNode(node.getLabel());
            link.setDstNode(node.getLabel());
            link.setMaximumResourceValue(ResourceType.Bandwidth, Integer.MAX_VALUE);
            link.setRemainingResourceValue(ResourceType.Bandwidth, Integer.MAX_VALUE);
            link.setMaximumResourceValue(ResourceType.Latency, 0);
            link.setRemainingResourceValue(ResourceType.Latency, 0);
            ng.getLinks().add(link);
            linkIndex++;
        }

        int i = 0;
        pointer = 0;
        while (true) {
            pointer = xml.indexOf("<edge ", pointer + 1);
            if (pointer < 0) {
                break;
            }
            int p = xml.indexOf("source=\"", pointer);
            String source = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            p = xml.indexOf("target=\"", pointer);
            String target = xml.substring(xml.indexOf("\"", p) + 1,
                    xml.indexOf("\"", xml.indexOf("\"", p) + 1));
            NetworkLink link = new NetworkLink();
            link.setLabel("L" + linkIndex);
            link.setSrcNode(source);
            link.setDstNode(target);
            link.setMaximumResourceValue(ResourceType.Bandwidth, bandwidth);
            link.setRemainingResourceValue(ResourceType.Bandwidth, bandwidth);
            link.setMaximumResourceValue(ResourceType.Latency, randomLatency ? (Math.abs(random.nextInt()) % latencyRange + latencyOffset) : latencyRange);
            link.setRemainingResourceValue(ResourceType.Latency, link.getMaximumResourceValue(ResourceType.Latency));
            ng.getLinks().add(link);
            linkIndex++;

            NetworkLink clone = link.clone();
            clone.setLabel("L" + linkIndex);
            clone.setSrcNode(link.getDstNode());
            clone.setDstNode(link.getSrcNode());
            clone.setMaximumResourceValue(ResourceType.Bandwidth, bandwidth);
            clone.setRemainingResourceValue(ResourceType.Bandwidth, bandwidth);
            clone.setMaximumResourceValue(ResourceType.Latency, randomLatency ? (Math.abs(random.nextInt()) % latencyRange + latencyOffset) : latencyRange);
            clone.setRemainingResourceValue(ResourceType.Latency, clone.getMaximumResourceValue(ResourceType.Latency));
            ng.getLinks().add(clone);
            linkIndex++;

            i++;
        }
        return ng;
    }
}
