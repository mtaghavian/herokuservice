package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.GraphUtils;
import com.bcom.nsplacer.placement.enums.ObjectiveType;
import com.bcom.nsplacer.placement.enums.ResourceType;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.routing.RoutingPath;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SearchState implements Comparable<SearchState> {

    public static final long routingTimeout = 2000;
    public static Random random = new Random(System.currentTimeMillis());
    private static DatagramSocket sock;
    private NetworkGraph networkGraph;
    private Double objectiveValue = 0.0;
    private Integer depth;
    private List<VNF> vnfList;
    private List<VNF> placedVNFs;
    private List<String> placedNodes;
    private List<VirtualLink> placedVLs;
    private List<RoutingPath> placedPaths;
    private Placer placer;
    private SearchStrategy strategy;
    private boolean checkOverallLatency;

    public SearchState(NetworkGraph networkGraph, List<VNF> vnfList, int depth, Placer placer, SearchStrategy strategy, boolean checkOverallLatency) {
        placer.totalCreatedStates++;
        this.placer = placer;
        this.strategy = strategy;
        this.networkGraph = networkGraph;
        this.depth = depth;
        this.vnfList = vnfList;
        this.checkOverallLatency = checkOverallLatency;
        if (depth == 0) {
            placedVNFs = new ArrayList<>();
            placedNodes = new ArrayList<>();
            placedVLs = new ArrayList<>();
            placedPaths = new ArrayList<>();
        }
    }

    public void updateObjectiveValue() {
        if (SearchStrategy.DBO.equals(strategy)) {
            if (ObjectiveType.Bandwidth.equals(placer.getObjectiveType())) {
                objectiveValue = (double) networkGraph.getTotalUsedResourceValue(false, ResourceType.Bandwidth);
            } else {
                int sum = 0;
                for (RoutingPath path : placedPaths) {
                    for (NetworkLink link : path.getLinks()) {
                        //sum += 1;
                        sum += link.getRemainingResourceValue(ResourceType.Latency);
                    }
                }
                objectiveValue = (double) sum;
            }
        } else if (SearchStrategy.ABO.equals(strategy)) {
            int unplacedBandwidth = 0;
            for (VirtualLink link : placer.getServiceGraph().getVirtualLinks()) {
                if (!placedVLs.contains(link)) {
                    unplacedBandwidth += link.getRequiredResourceValue(ResourceType.Bandwidth);
                }
            }
            int usedBandwidth = 0;
            for (int i = 0; i < placedVLs.size(); i++) {
                usedBandwidth += placedVLs.get(i).getRequiredResourceValue(ResourceType.Bandwidth) * placedPaths.get(i).getLinks().size();
            }
            objectiveValue = (double) usedBandwidth + unplacedBandwidth;
        } else if (SearchStrategy.SAP.equals(strategy)) {
            try {
                if (sock == null) {
                    sock = new DatagramSocket(60101);
                    sock.setSoTimeout(2000);
                }

                StringBuilder sb = new StringBuilder();
                //Collections.sort(networkGraph.getNodes());
                //Map<String, Integer> map = networkGraph.mapOfAggregatedRemainingBandwidth();
                networkGraph.sortNodes();
                List<Integer> list = networkGraph.listOfAggregatedRemainingBandwidth();
                for (int i = 0; i < networkGraph.getNodes().size(); i++) {
                    sb.append((i != 0) ? ";" : "");
                    sb.append(networkGraph.getNodes().get(i).getRemainingResourceValue(ResourceType.Cpu));
                    sb.append(";");
                    sb.append(networkGraph.getNodes().get(i).getRemainingResourceValue(ResourceType.Storage));
                    //sb.append(";").append(map.get(networkGraph.getNodes().get(i).getLabel()));
                }
                for (int l : list) {
                    sb.append(";").append(l);
                }

                byte outputData[] = sb.toString().getBytes();
                DatagramPacket outputPacket = new DatagramPacket(outputData, outputData.length);
                outputPacket.setAddress(InetAddress.getByName("127.0.0.1"));
                outputPacket.setPort(60102);
                sock.send(outputPacket);

                byte inputData[] = new byte[1000];
                DatagramPacket inputPacket = new DatagramPacket(inputData, inputData.length);
                sock.receive(inputPacket);
                objectiveValue = Double.parseDouble(new String(inputData, 0, inputPacket.getLength(), "UTF-8"));
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        } else {
            objectiveValue = 0.0;
        }
    }

    public List<SearchState> expand() {
        if (SearchStrategy.EDFF.equals(strategy) || SearchStrategy.EIFF.equals(strategy)) {
            if (placer.isShuffle()) {
                Collections.shuffle(networkGraph.getNodes());
            }
            Collections.sort(networkGraph.getNodes(), (o1, o2) -> {
                double sum = 0;
                for (Resource r : o1.getRemainingResources()) {
                    sum += ((double) o1.getRemainingResourceValue(r.getType()) / o1.getMaximumResourceValue(r.getType())) -
                            ((double) o2.getRemainingResourceValue(r.getType()) / o2.getMaximumResourceValue(r.getType()));
                }
                int c = (int) Math.round(Math.signum(sum));
                return SearchStrategy.EDFF.equals(strategy) ? c : -c;
            });
        } else if (SearchStrategy.Random.equals(strategy)) {
            Collections.shuffle(networkGraph.getNodes());
        }

        List<SearchState> children = new ArrayList<>();
        VNF placingVNF = vnfList.get(depth);

        for (NetworkNode placingNode : networkGraph.getNodes()) {
            NetworkGraph graph = networkGraph.clone();
            for (NetworkNode node : graph.getNodes()) {
                if (node.getLabel().equals(placingNode.getLabel())) {
                    placingNode = node;
                }
            }

            if (!placingNode.canAccommodate(placingVNF) || placedNodes.contains(placingNode.getLabel())) {
                continue;
            }

            SearchState child = new SearchState(graph, vnfList, depth + 1, placer, strategy, checkOverallLatency);
            child.setPlacedNodes(CollectionUtils.concat(new ArrayList<>(placedNodes), placingNode.getLabel()));
            child.setPlacedVNFs(CollectionUtils.concat(new ArrayList<>(placedVNFs), placingVNF));
            child.setPlacedVLs(new ArrayList<>(placedVLs));
            child.setPlacedPaths(new ArrayList<>(placedPaths));

            // Perform placement for the VNF
            for (Resource r : placingNode.getRemainingResources()) {
                placingNode.setRemainingResourceValue(r.getType(), placingNode.getRemainingResourceValue(r.getType())
                        - placingVNF.getRequiredResourceValue(r.getType()));
            }

            // Perform placement for the Virtual Link
            HashMap<String, Integer> placedVLMap = new HashMap<>();
            for (int i = 0; i < placedVLs.size(); i++) {
                placedVLMap.put(placedVLs.get(i).getLabel(), i);
            }
            HashMap<String, Integer> placedVNFMap = new HashMap<>();
            for (int i = 0; i < child.getPlacedVNFs().size(); i++) {
                placedVNFMap.put(child.getPlacedVNFs().get(i).getLabel(), i);
            }

            List<VirtualLink> vlsNeedToBePlaced = new ArrayList<>();
            List<List<RoutingPath>> candidatePaths = new ArrayList<>();
            boolean routingFailed = false;
            for (VirtualLink vl : placer.getServiceGraph().getVirtualLinks()) {
                if (!placedVLMap.containsKey(vl.getLabel())) {
                    if (placedVNFMap.containsKey(vl.getSrcVNF()) && placedVNFMap.containsKey(vl.getDstVNF())) {
                        vlsNeedToBePlaced.add(vl);
                        String srcNode = child.getPlacedNodes().get(placedVNFMap.get(vl.getSrcVNF()));
                        String dstNode = child.getPlacedNodes().get(placedVNFMap.get(vl.getDstVNF()));
                        List<RoutingPath> routingPaths = placer.getRoutingAlgorithm().route(child, srcNode, dstNode, vl, routingTimeout);
                        candidatePaths.add(routingPaths);

                        // For performance reasons
                        // If we could not find any path for a VL
                        // This placement is not feasible and we can skip placing other VLs
                        if (routingPaths.isEmpty()) {
                            routingFailed = true;
                            child.getPlacedVLs().add(vl);
                            child.getPlacedPaths().add(new RoutingPath());
                            break;
                        }
                    }
                }
            }
            if (!routingFailed && !vlsNeedToBePlaced.isEmpty()) {
                Map<String, Integer> remaining = new HashMap<>(), demanding = new HashMap<>();
                for (NetworkLink link : graph.getLinks()) {
                    remaining.put(link.getLabel(), link.getRemainingResourceValue(ResourceType.Bandwidth));
                }
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < candidatePaths.size(); i++) {
                    indices.add(0);
                }
                for (NetworkLink link : graph.getLinks()) {
                    demanding.put(link.getLabel(), 0);
                }
                for (int i = 0; i < candidatePaths.size(); i++) {
                    RoutingPath path = candidatePaths.get(i).get(indices.get(i));
                    for (NetworkLink link : path.getLinks()) {
                        demanding.put(link.getLabel(), demanding.get(link.getLabel()) + vlsNeedToBePlaced.get(i).getRequiredResourceValue(ResourceType.Bandwidth));
                    }
                }
                boolean dependencyOk = true;
                for (String link : remaining.keySet()) {
                    if (remaining.get(link) < demanding.get(link)) {
                        dependencyOk = false;
                        break;
                    }
                }
                if (dependencyOk) {
                    for (int i = 0; i < candidatePaths.size(); i++) {
                        RoutingPath path = candidatePaths.get(i).get(indices.get(i));
                        for (NetworkLink link : path.getLinks()) {
                            link.setRemainingResourceValue(ResourceType.Bandwidth,
                                    link.getRemainingResourceValue(ResourceType.Bandwidth)
                                            - vlsNeedToBePlaced.get(i).getRequiredResourceValue(ResourceType.Bandwidth));
                        }
                        child.getPlacedVLs().add(vlsNeedToBePlaced.get(i));
                        child.getPlacedPaths().add(path);
                    }
                } else {
                    System.out.println("Conflicts in routings!");
                    child.getPlacedVLs().add(vlsNeedToBePlaced.get(0));
                    child.getPlacedPaths().add(new RoutingPath());
                }
            }

            // Update objective value
            if (child.isFeasible()) {
                placer.totalFeasibleStates++;
                child.updateObjectiveValue();
                children.add(child);
            }
        }
        if (SearchStrategy.SAP.equals(strategy) || SearchStrategy.DBO.equals(strategy)) {
            if (placer.isShuffle()) {
                Collections.shuffle(children);
            }
            Collections.sort(children);
        }
        if (!placer.isBacktracking()) {
            if (!children.isEmpty()) {
                SearchState lastChild = children.get(children.size() - 1);
                children.clear();
                children.add(lastChild);
            }
        }
        return children;
    }

    public Map<String, String> getPlacementNodeMap() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < vnfList.size(); i++) {
            map.put(vnfList.get(i).getLabel(), placedNodes.get(i));
        }
        return map;
    }

    public Map<String, String> getPlacementLinkMap() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < placedVLs.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(placedPaths.get(i).getLinks().get(0).getLabel());
            for (int j = 1; j < placedPaths.get(i).getLinks().size(); j++) {
                sb.append("-" + placedPaths.get(i).getLinks().get(j).getLabel());
            }
            map.put(placedVLs.get(i).getLabel(), sb.toString());
        }
        return map;
    }

    public int calcPlacedLatency() {
        int sum = 0;
        for (RoutingPath path : placedPaths) {
            for (NetworkLink l : path.getLinks()) {
                sum += l.getRemainingResourceValue(ResourceType.Latency);
            }
        }
        return sum;
    }

    @Override
    public int compareTo(SearchState o) {
        int cmp = objectiveValue.compareTo(o.getObjectiveValue());
        if (SearchStrategy.ABO.equals(strategy)) {
            if (cmp != 0) {
                return cmp;
            }
            cmp = -depth.compareTo(o.depth);
            if (cmp != 0) {
                return cmp;
            }
            cmp = -(GraphUtils.minimumRemainingBandwidth(networkGraph.getLinks()))
                    .compareTo(GraphUtils.minimumRemainingBandwidth(o.getNetworkGraph().getLinks()));
            if (cmp != 0) {
                return cmp;
            }
//            cmp = -(GraphUtils.maximumRemainingBandwidth(networkGraph.getLinks()))
//                    .compareTo(GraphUtils.maximumRemainingBandwidth(o.getNetworkGraph().getLinks()));
//            if (cmp != 0) {
//                return cmp;
//            }
//            cmp = -(GraphUtils.averageRemainingBandwidth(networkGraph.getLinks()))
//                    .compareTo(GraphUtils.averageRemainingBandwidth(o.getNetworkGraph().getLinks()));
//            if (cmp != 0) {
//                return cmp;
//            }
            //System.out.println("hey: " + depth);
            //return (o.calcPlacedLatency() - calcPlacedLatency()) * 1;
            return 0;
        } else if (SearchStrategy.DBO.equals(strategy)) {
            return -cmp;
        } else {
            return cmp;
        }
    }

    public boolean isFeasible() {
        // Check infeasibility for nodes
        for (int i = 0; i < networkGraph.getNodes().size(); i++) {
            if (!networkGraph.getNodes().get(i).checkFeasibility()) {
                return false;
            }
        }
        // Check infeasibility for links
        for (RoutingPath p : placedPaths) {
            if (p.getLinks() == null || p.getLinks().isEmpty()) {
                return false;
            }
        }
        // Check end-to-end latency
        if (checkOverallLatency && isTerminal()) {
            int requiredLatency = 0;
            for (VirtualLink vl : placer.getServiceGraph().getVirtualLinks()) {
                requiredLatency += vl.getRequiredResourceValue(ResourceType.Latency);
            }
            int providedLatency = 0;
            for (RoutingPath p : placedPaths) {
                for (NetworkLink l : p.getLinks()) {
                    providedLatency += l.getRemainingResourceValue(ResourceType.Latency);
                }
            }
            if (requiredLatency < providedLatency) {
                return false;
            }
        }
        return true;
    }

    public boolean isTerminal() {
        return depth == vnfList.size();
    }
}
