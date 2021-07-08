package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.misc.CollectionUtils;
import com.bcom.nsplacer.misc.FWRouting;
import com.bcom.nsplacer.misc.GraphUtils;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import com.bcom.nsplacer.placement.SearchState;
import com.bcom.nsplacer.placement.VirtualLink;
import com.bcom.nsplacer.placement.enums.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Limited Depth First Search Routing Algorithm
 */
public class IDSRoutingAlgorithm extends RoutingAlgorithm {

    public static int FLOOR = 0;
    private HashSet<String> pathSet;
    private FWRouting fwRouting;
    private List<RoutingPath> foundPaths;
    private int foundPathLatency;
    private boolean terminateCommand;
    private long beginTime;

    public IDSRoutingAlgorithm(NetworkGraph graph) {
        fwRouting = new FWRouting();
        fwRouting.build(graph);
        setMaxNumPaths(10);
        setConsiderLatency(true);
    }

    @Override
    public List<RoutingPath> route(SearchState state, String srcNode, String dstNode, VirtualLink vl, long timeout) {
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
            while ((foundPaths.size() < getMaxNumPaths()) && !terminateCommand
                    //&& (depthLimit <= fwRouting.getMaxDist())
                    && (depthLimit <= fwRouting.getDist(srcNode, dstNode) + 1)
                    && (System.currentTimeMillis() - beginTime) < timeout) {
                traverse(state, depthLimit++, new ArrayList<>(), srcNode, dstNode, vl);
                //if (!foundPaths.isEmpty()) {
                //    break;
                //}
            }

            if (foundPaths.size() > 1) {
                //filter();
                sort();
            }
            return foundPaths;
        }
    }

    private void filter() {
        int minLength = foundPaths.get(0).getLinks().size();
        int allowedLengthVariation = 2;
        while (foundPaths.get(foundPaths.size() - 1).getLinks().size() > minLength + allowedLengthVariation) {
            foundPaths.remove(foundPaths.size() - 1);
        }
    }

    private void sort() {
        //Collections.sort(foundPaths, (o1, o2) -> -GraphUtils.averageRemainingBandwidth(o1.getLinks()).compareTo(GraphUtils.averageRemainingBandwidth(o2.getLinks())));
        //Collections.sort(foundPaths, (o1, o2) -> -GraphUtils.maximumRemainingBandwidth(o1.getLinks()).compareTo(GraphUtils.maximumRemainingBandwidth(o2.getLinks())));
        Collections.sort(foundPaths, (o1, o2) -> -GraphUtils.minimumRemainingBandwidth(o1.getLinks()).compareTo(GraphUtils.minimumRemainingBandwidth(o2.getLinks())));
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
            List<NetworkLink> links = state.getNetworkGraph().getLinks();
            //links = new ArrayList<>(links);
            //Collections.shuffle(links);
            //Collections.sort(links, (o1, o2) -> -(o1.getRemainingResourceValue(ResourceType.Bandwidth) - o2.getRemainingResourceValue(ResourceType.Bandwidth)));
            for (NetworkLink link : links) {
                if (link.getSrcNode().equals(srcNode) && !link.isLoop()
                        && (link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        && (!isConsiderLatency() || checkPathLatency(path, link, vl))
                        && fwRouting.getDist(link.getDstNode(), dstNode) < depthLimit
                        && !hasLoop(path, link)
                        && link.getRemainingResourceValue(ResourceType.Bandwidth) > FLOOR
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
