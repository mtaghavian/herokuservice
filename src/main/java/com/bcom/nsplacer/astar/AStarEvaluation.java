package com.bcom.nsplacer.astar;

import com.bcom.nsplacer.misc.FWRouting;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.routing.IDSRoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.RoutingPath;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AStarEvaluation {

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

        AStarUtils.search(new NetworkNodeState(networkGraph.getNodes().get(6).getLabel(), networkGraph.getNodes().get(12).getLabel(), fwRouting, vl), 1000000, new AStarTargetAction() {
            @Override
            public boolean perform(AStarState target) {
                System.out.println("" + (System.currentTimeMillis() - time) + "\t" + pathCounter + "\t" + (target.getPath().size() - 1) + "\t" + "\"" + getSearchPath(target) + "\"");
                pathCounter++;
                return false;
            }
        });
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
        List<RoutingPath> paths = idsRoutingAlgorithm.route(searchState, networkGraph.getNodes().get(6).getLabel(), networkGraph.getNodes().get(12).getLabel(), vl);
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
        evaluateIDS();
    }

    public static String getSearchPath(AStarState state) {
        List<AStarState> path = state.getPath();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            NetworkNodeState ms = (NetworkNodeState) path.get(i);
            if (i != 0) {
                sb.append(" > ");
            }
            sb.append(ms.getCurrentNode());
        }
        return sb.toString();
    }

    @Getter
    @Setter
    public static class NetworkNodeState extends AStarState {

        private String currentNode, targetNode;
        private FWRouting fwAlgorithm;
        private VirtualLink vl;

        public NetworkNodeState(String currentNode, String targetNode, FWRouting fwAlgorithm, VirtualLink vl) {
            this.currentNode = currentNode;
            this.targetNode = targetNode;
            this.fwAlgorithm = fwAlgorithm;
            this.vl = vl;
        }

        @Override
        public boolean isTarget() {
            return currentNode.equals(targetNode);
        }

        @Override
        public int gCost() {
            return getPath().size() - 1;
        }

        @Override
        public int hCost() {
            return fwAlgorithm.getDist(currentNode, targetNode);
        }

        @Override
        public List<AStarState> expand() {
            List<AStarState> list = new ArrayList<>();
            for (NetworkLink link : fwAlgorithm.getGraph().getLinks()) {
                if (link.getSrcNode().equals(currentNode) && !link.isLoop()
                        && (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        // && (!assertLatency || checkPathLatency(path, link, vl))
                        && !hasLoop(link.getDstNode())
                ) {
                    list.add(new NetworkNodeState(link.getDstNode(), targetNode, fwAlgorithm, vl));
                }
            }
            return list;
        }

        private boolean hasLoop(String dst) {
            for (AStarState s : getPath()) {
                if (((NetworkNodeState) s).getCurrentNode().equals(dst)) {
                    return true;
                }
            }
            return false;
        }
    }
}
