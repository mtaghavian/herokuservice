package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.FWRouting;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.SearchState;
import com.bcom.nsplacer.placement.VirtualLink;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.util.*;

/**
 * Limited Depth First Search Routing Algorithm
 */
public class IDSRoutingAlgorithm extends RoutingAlgorithm {

    private HashSet<String> pathSet;
    private FWRouting fwRouting;
    private List<RoutingPath> foundPaths;
    private int foundPathLatency;
    private boolean terminateCommand;
    private long beginTime;

    public IDSRoutingAlgorithm(NetworkGraph graph) {
        fwRouting = new FWRouting();
        fwRouting.build(graph);
        setMaxNumPaths(100);
        setConsiderLatency(true);
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, VirtualLink vl) {
        if (srcNode.equals(dstNode)) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        && link.isLoop() && link.getSrcNode().equals(srcNode)) {
                    List<RoutingPath> paths = new ArrayList<>();
                    paths.add(new RoutingPath(CollectionUtils.concat(new ArrayList<>(), link)));
                    return paths;
                }
            }
            return new ArrayList<>();
        } else {
            pathSet = new HashSet<>();
            beginTime = System.currentTimeMillis();
            terminateCommand = false;
            foundPaths = new ArrayList<>();
            int depthLimit = fwRouting.getDist(srcNode, dstNode);
            while ((foundPaths.size() < getMaxNumPaths()) && !terminateCommand && (depthLimit <= fwRouting.getMaxDist())) {
                traverse(state, depthLimit++, new ArrayList<>(), srcNode, dstNode, vl);
            }
            if (!foundPaths.isEmpty()) {
//                int minLength = foundPaths.get(0).getLinks().size();
//                int allowedLengthVariation = 2;
//                while (foundPaths.get(foundPaths.size() - 1).getLinks().size() > minLength + allowedLengthVariation) {
//                    foundPaths.remove(foundPaths.size() - 1);
//                }
                Collections.sort(foundPaths, new Comparator<RoutingPath>() {
                    @Override
                    public int compare(RoutingPath o1, RoutingPath o2) {
                        return (int) Math.signum(criteria(o2.getLinks()) - criteria(o1.getLinks()));
                    }

                    public double criteria(List<NetworkLink> links) {
                        return minRemainingBandwidth(links);
//                        List<Double> costs = new ArrayList<>();
//                        for (NetworkLink link : links) {
//                            costs.add((double) link.getMaximumResourceValue(ResourceType.Bandwidth) / link.getRemainingResourceValue(ResourceType.Bandwidth));
//                        }
//                        return MathUtils.sum(costs);
                    }

                    private double maxRemainingBandwidth(List<NetworkLink> links) {
                        double d = 0;
                        for (NetworkLink link : links) {
                            d = Math.max(d, link.getRemainingResourceValue(ResourceType.Bandwidth));
                        }
                        return d;
                    }

                    private double minRemainingBandwidth(List<NetworkLink> links) {
                        double d = Integer.MAX_VALUE;
                        for (NetworkLink link : links) {
                            d = Math.min(d, link.getRemainingResourceValue(ResourceType.Bandwidth));
                        }
                        return d;
                    }

                    private double avgRemainingBandwidth(List<NetworkLink> links) {
                        double d = 0.0;
                        for (NetworkLink link : links) {
                            d += link.getRemainingResourceValue(ResourceType.Bandwidth);
                        }
                        d /= links.size();
                        return d;
                    }
                });
            }
            return foundPaths;
        }
    }

    private void traverse(SearchState state, int depthLimit, List<NetworkLink> path, String srcNode, String dstNode, VirtualLink vl) {
        if (terminateCommand) {
            return;
        } else if (srcNode.equals(dstNode)) {
            String pathString = pathToString(path);
            if (!pathSet.contains(pathString)) {
                int pathLatency = calcPathLatency(path);
                pathSet.add(pathString);
                //System.out.println("" + (System.currentTimeMillis() - beginTime) + "\t" + foundPathCount + "\t" + path.size() + "\t" + "\"" + pathString + "\"");

                if (!isConsiderLatency()) {
                    foundPaths.add(new RoutingPath(path));
                    terminateCommand = true;
                } else {
                    if (vl.getRequiredResourceValue(ResourceType.Latency) >= pathLatency) {
                        foundPaths.add(new RoutingPath(path));
                        if (foundPaths.size() == getMaxNumPaths()) {
                            terminateCommand = true;
                        }
                    }
                }
            }
        } else if (depthLimit == 0) {
            return;
        } else {
            //List<NetworkLink> links = new ArrayList<>(state.getNetworkGraph().getLinks());
            //Collections.shuffle(links);
            //Collections.sort(links, (o1, o2) -> o2.getRemainingResourceValue(ResourceType.Bandwidth) - o1.getRemainingResourceValue(ResourceType.Bandwidth));
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if (link.getSrcNode().equals(srcNode) && !link.isLoop() &&
                        (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth)) &&
                        (!isConsiderLatency() || checkPathLatency(path, link, vl)) &&
                        fwRouting.getDist(link.getDstNode(), dstNode) < depthLimit &&
                        !hasLoop(path, link)
                ) {
                    traverse(state, depthLimit - 1, CollectionUtils.concat(new ArrayList<>(path), link), link.getDstNode(), dstNode, vl);
                }
            }
        }
    }

    private String pathToString(List<NetworkLink> path) {
        StringBuilder sb = new StringBuilder();
        for (NetworkLink l : path) {
            sb.append("" + l.getSrcNode() + " > ");
        }
        sb.append("" + path.get(path.size() - 1).getDstNode() + "");
        return sb.toString();
    }

    private int calcPathLatency(List<NetworkLink> path) {
        int sum = 0;
        for (NetworkLink l : path) {
            sum += l.getRemainingResourceValue(ResourceType.Latency);
        }
        return sum;
    }

    private boolean checkPathLatency(List<NetworkLink> path, NetworkLink link, VirtualLink vl) {
        int sum = calcPathLatency(path);
        sum += link.getRemainingResourceValue(ResourceType.Latency);
        return sum <= vl.getRequiredResourceValue(ResourceType.Latency);
    }

    private boolean hasLoop(List<NetworkLink> path, NetworkLink link) {
        for (NetworkLink p : path) {
            if (p.getSrcNode().equals(link.getDstNode())) {
                return true;
            }
        }
        return false;
    }

}
