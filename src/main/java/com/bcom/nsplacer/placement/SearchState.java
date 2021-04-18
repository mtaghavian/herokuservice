package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.CollectionUtils;
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

    public static Random random = new Random(System.currentTimeMillis());
    private static DatagramSocket sock;
    private NetworkGraph networkGraph;
    private Double optimizerValue = 0.0;
    private Integer depth;
    private List<VNF> vnfList;
    private List<VNF> placedVNFs;
    private List<NetworkNode> placedNodes;
    private List<VirtualLink> placedVLs;
    private List<List<NetworkLink>> placedPaths;
    private RoutingAlgorithm routingAlgorithm;
    private Placer placer;
    private PlacerStrategy strategy;

    public SearchState(NetworkGraph networkGraph, List<VNF> vnfList, int depth, Placer placer, PlacerStrategy s) {
        placer.totalCreatedStates++;
        this.placer = placer;
        this.strategy = s;
        this.networkGraph = networkGraph;
        this.depth = depth;
        this.vnfList = vnfList;
        if (depth == 0) {
            placedVNFs = new ArrayList<>();
            placedNodes = new ArrayList<>();
            placedVLs = new ArrayList<>();
            placedPaths = new ArrayList<>();
        }
        if (RoutingType.HopCount.equals(placer.getRoutingType())) {
            routingAlgorithm = new HopCountRoutingAlgorithm();
        } else if (RoutingType.SAMCRA.equals(placer.getRoutingType())) {
            // Implement SAMCRA routing algorithm
        }
    }

    public void updateOptimizerValue() {
        if (PlacerStrategy.DBO.equals(strategy)) {
            optimizerValue = (double) networkGraph.getTotalRemainingResourceValue(false, ResourceType.Bandwidth);
        } else if (PlacerStrategy.ABO.equals(strategy)) {
            int unplacedBandwidth = 0;
            for (VirtualLink link : placer.getServiceGraph().getVirtualLinks()) {
                if (!placedVLs.contains(link)) {
                    unplacedBandwidth += link.getRequiredResourceValue(ResourceType.Bandwidth);
                }
            }
            int usedBandwidth = 0;
            for (int i = 0; i < placedVLs.size(); i++) {
                usedBandwidth += placedVLs.get(i).getRequiredResourceValue(ResourceType.Bandwidth) * placedPaths.get(i).size();
            }
            optimizerValue = (double) usedBandwidth + unplacedBandwidth;
        } else if (PlacerStrategy.SAP.equals(strategy)) {
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
                optimizerValue = Double.parseDouble(new String(inputData, 0, inputPacket.getLength(), "UTF-8"));
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        } else {
            optimizerValue = 0.0;
        }
    }

    public List<SearchState> expand() {
        List<SearchState> children = new ArrayList<>();
        VNF placingVNF = vnfList.get(depth);
        if (PlacerStrategy.EDFF.equals(strategy) || PlacerStrategy.EIFF.equals(strategy)) {
            Collections.sort(networkGraph.getNodes(), (o1, o2) -> {
                double sum = 0;
                for (Resource r : o1.getRemainingResources()) {
                    sum += ((double) o1.getRemainingResourceValue(r.getType()) / o1.getMaximumResourceValue(r.getType())) -
                            ((double) o2.getRemainingResourceValue(r.getType()) / o2.getMaximumResourceValue(r.getType()));
                }
                int c = (int) Math.round(Math.signum(sum));
                return PlacerStrategy.EDFF.equals(strategy) ? c : -c;
            });
        } else if (PlacerStrategy.Random.equals(strategy)) {
            Collections.shuffle(networkGraph.getNodes());
        }

        for (int index = 0; index < networkGraph.getNodes().size(); index++) {
            NetworkGraph graph = networkGraph.clone();
            NetworkNode placingNode = graph.getNodes().get(index);

            if (!placingNode.canAccommodate(placingVNF) || placedNodes.contains(placingNode)) {
                continue;
            }

            SearchState child = new SearchState(graph, vnfList, depth + 1, placer, strategy);
            child.setPlacedNodes(CollectionUtils.concat(new ArrayList<>(placedNodes), placingNode));
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

            for (VirtualLink vl : placer.getServiceGraph().getVirtualLinks()) {
                if (!placedVLMap.containsKey(vl.getLabel())) {
                    if (placedVNFMap.containsKey(vl.getSrcVNF()) && placedVNFMap.containsKey(vl.getDstVNF())) {
                        NetworkNode srcNode = child.getPlacedNodes().get(placedVNFMap.get(vl.getSrcVNF()));
                        NetworkNode dstNode = child.getPlacedNodes().get(placedVNFMap.get(vl.getDstVNF()));

                        List<NetworkLink> path = routingAlgorithm.route(child, srcNode, dstNode, vl);
                        for (NetworkLink link : path) {
                            link.setRemainingResourceValue(ResourceType.Bandwidth,
                                    link.getRemainingResourceValue(ResourceType.Bandwidth)
                                            - vl.getRequiredResourceValue(ResourceType.Bandwidth));
                        }
                        child.getPlacedVLs().add(vl);
                        child.getPlacedPaths().add(path);

                        // For performance reasons
                        // If we could not find any path for a VL
                        // This placement is not feasible and we can skip placing other VLs
                        if (path.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // Update optimizer value
            if (child.isFeasible()) {
                placer.totalFeasibleStates++;
                child.updateOptimizerValue();
                children.add(child);
            }
        }
        if (PlacerStrategy.SAP.equals(strategy) || PlacerStrategy.DBO.equals(strategy)) {
            Collections.sort(children);
        }
        if (!placer.isRecursive()) {
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
            map.put(vnfList.get(i).getLabel(), placedNodes.get(i).getLabel());
        }
        return map;
    }

    public Map<String, String> getPlacementLinkMap() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < placedVLs.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(placedPaths.get(i).get(0).getLabel());
            for (int j = 1; j < placedPaths.get(i).size(); j++) {
                sb.append("-" + placedPaths.get(i).get(j).getLabel());
            }
            map.put(placedVLs.get(i).getLabel(), sb.toString());
        }
        return map;
    }

    @Override
    public int compareTo(SearchState o) {
        int c = optimizerValue.compareTo(o.getOptimizerValue());
        if ((PlacerStrategy.ABO).equals(strategy)) {
            if (c == 0) {
                return -depth.compareTo(o.depth);
            }
        }
        return c;
    }

    public boolean isFeasible() {
        // Check infeasibility for nodes
        for (int i = 0; i < networkGraph.getNodes().size(); i++) {
            if (!networkGraph.getNodes().get(i).checkFeasibility()) {
                return false;
            }
        }
        // Check infeasibility for links
        for (int i = 0; i < placedPaths.size(); i++) {
            if (placedPaths.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isTerminal() {
        return depth == vnfList.size();
    }
}
