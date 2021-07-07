package com.bcom.nsplacer.astar;

import com.bcom.nsplacer.misc.FWRouting;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.routing.IDSRoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.RoutingPath;

import java.io.File;
import java.util.List;
import java.util.Random;

public class RoutingEvaluation {

    public static int timeout = 100000;
    public static int CpuDemand = 1;
    public static int StorageDemand = 1;
    public static int BandwidthAvailable = 10;
    public static int latencyRange = 100, latencyOffset = 1, maxLatencyDemand = 30;
    public static int nodeCpuAndStorageResources = 1000000;
    public static int pathCounter = 1;

    public static void evaluateAStar() throws Exception {
        String SNTopology = "./samples of network graphs/zoo-topologies/BtNorthAmerica.graphml.xml";
        NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(new Random(1), StreamUtils.readString(new File(SNTopology)), nodeCpuAndStorageResources, nodeCpuAndStorageResources, BandwidthAvailable, true, latencyRange, latencyOffset);
        FWRouting fwRouting = new FWRouting();
        fwRouting.build(networkGraph);
        VirtualLink vl = new VirtualLink();
        vl.setRequiredResourceValue(ResourceType.Bandwidth, 1);
        long time = System.currentTimeMillis();

        SearchState searchState = new SearchState();
        searchState.setNetworkGraph(networkGraph);

        AStarRoutingAlgorithm routing = new AStarRoutingAlgorithm(networkGraph);
        List<RoutingPath> route = routing.route(searchState, networkGraph.getNodes().get(6).getLabel(), networkGraph.getNodes().get(12).getLabel(), vl, 4000);
        for (int i = 0; i < route.get(0).getLinks().size(); i++) {
            if (i == 0) {
                System.out.println(route.get(0).getLinks().get(0).getSrcNode());
            }
            System.out.println(route.get(0).getLinks().get(i).getDstNode());
        }
    }

    public static void evaluateIDS() throws Exception {
        String SNTopology = "./samples of network graphs/zoo-topologies/BtNorthAmerica.graphml.xml";
        NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(null, StreamUtils.readString(new File(SNTopology)), nodeCpuAndStorageResources, nodeCpuAndStorageResources, BandwidthAvailable, false, 1, 0);

        IDSRoutingAlgorithm idsRoutingAlgorithm = new IDSRoutingAlgorithm(networkGraph);
        idsRoutingAlgorithm.setConsiderLatency(true);
        idsRoutingAlgorithm.setMaxNumPaths(10);
        SearchState searchState = new SearchState();
        searchState.setNetworkGraph(networkGraph);

        VirtualLink vl = new VirtualLink();
        vl.setRequiredResourceValue(ResourceType.Bandwidth, 1);
        vl.setRequiredResourceValue(ResourceType.Latency, 100);
        List<RoutingPath> paths = idsRoutingAlgorithm.route(searchState, networkGraph.getNodes().get(6).getLabel(), networkGraph.getNodes().get(12).getLabel(), vl, 2000);
        int cnt = 0;
        for (RoutingPath p : paths) {
            System.out.println("" + (++cnt) + ") " + pathToString(p.getLinks()));
        }
    }

    public static void evaluateDijkstra() throws Exception {
        String SNTopology = "./samples of network graphs/zoo-topologies/BtNorthAmerica.graphml.xml";
        NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(null, StreamUtils.readString(new File(SNTopology)), nodeCpuAndStorageResources, nodeCpuAndStorageResources, BandwidthAvailable, false, 1, 0);

        DijkstraRoutingAlgorithm routingAlgorithm = new DijkstraRoutingAlgorithm(networkGraph);
        SearchState searchState = new SearchState();
        searchState.setNetworkGraph(networkGraph);

        VirtualLink vl = new VirtualLink();
        vl.setRequiredResourceValue(ResourceType.Bandwidth, 1);
        vl.setRequiredResourceValue(ResourceType.Latency, 100);
        List<RoutingPath> paths = routingAlgorithm.route(searchState, "0", "1", vl, 2000);
        int cnt = 0;
        for (RoutingPath p : paths) {
            System.out.println("" + (++cnt) + ") " + pathToString(p.getLinks()));
        }
    }

    private static String pathToString(List<NetworkLink> path) {
        StringBuilder sb = new StringBuilder();
        for (NetworkLink l : path) {
            sb.append("" + l.getSrcNode() + " > ");
        }
        sb.append("" + path.get(path.size() - 1).getDstNode() + "");
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        //evaluateAStar();
        //evaluateIDS();
        evaluateDijkstra();
    }

}
