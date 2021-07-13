package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.InitializerParameters;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.io.IOException;

public class ZooTopologyImportExportManager {

    public static NetworkGraph importFromXML(String xml, InitializerParameters cpu, InitializerParameters storage, InitializerParameters bandwidth, InitializerParameters latency) throws IOException {
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
            node.setMaximumResourceValue(ResourceType.Cpu, cpu.get());
            node.setCurrentResourceValue(ResourceType.Cpu, node.getMaximumResourceValue(ResourceType.Cpu));
            node.setMaximumResourceValue(ResourceType.Storage, storage.get());
            node.setCurrentResourceValue(ResourceType.Storage, node.getMaximumResourceValue(ResourceType.Cpu));
            node.setLabel(label);
            ng.getNodes().add(node);

            NetworkLink link = new NetworkLink();
            link.setLabel("L" + linkIndex);
            link.setSrcNode(node.getLabel());
            link.setDstNode(node.getLabel());
            link.setMaximumResourceValue(ResourceType.Bandwidth, Integer.MAX_VALUE);
            link.setCurrentResourceValue(ResourceType.Bandwidth, Integer.MAX_VALUE);
            link.setMaximumResourceValue(ResourceType.Latency, 0);
            link.setCurrentResourceValue(ResourceType.Latency, 0);
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
            link.setMaximumResourceValue(ResourceType.Bandwidth, bandwidth.get());
            link.setCurrentResourceValue(ResourceType.Bandwidth, link.getMaximumResourceValue(ResourceType.Bandwidth));
            link.setMaximumResourceValue(ResourceType.Latency, latency.get());
            link.setCurrentResourceValue(ResourceType.Latency, link.getMaximumResourceValue(ResourceType.Latency));
            ng.getLinks().add(link);
            linkIndex++;

            NetworkLink clone = link.clone();
            clone.setLabel("L" + linkIndex);
            clone.setSrcNode(link.getDstNode());
            clone.setDstNode(link.getSrcNode());
            clone.setMaximumResourceValue(ResourceType.Bandwidth, bandwidth.get());
            clone.setCurrentResourceValue(ResourceType.Bandwidth, clone.getMaximumResourceValue(ResourceType.Bandwidth));
            clone.setMaximumResourceValue(ResourceType.Latency, latency.get());
            clone.setCurrentResourceValue(ResourceType.Latency, clone.getMaximumResourceValue(ResourceType.Latency));
            ng.getLinks().add(clone);
            linkIndex++;

            i++;
        }
        return ng;
    }
}
