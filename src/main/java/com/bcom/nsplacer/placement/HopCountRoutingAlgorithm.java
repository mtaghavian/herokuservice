package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.CollectionUtils;

import java.util.*;

public class HopCountRoutingAlgorithm implements RoutingAlgorithm{

    @Override
    public List<NetworkLink> route(SearchState state, NetworkNode srcNode, NetworkNode dstNode, VirtualLink vl) {
        if (srcNode.getLabel().equals(dstNode.getLabel())) {
            for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                if ((link.getRemainingResourceValue(ResourceType.Bandwidth) >= vl.getRequiredResourceValue(ResourceType.Bandwidth))
                        && link.isLoop() && link.getSrcNode().equals(srcNode.getLabel())) {
                    return CollectionUtils.concat(new ArrayList<>(), link);
                }
            }
            return new ArrayList<>();
        } else {
            List<NetworkNode> bfsList = new ArrayList<>();
            Set<String> bfsBag = new HashSet<>();
            Map<String, NetworkNode> map = new HashMap<>();
            for (NetworkNode node : state.getNetworkGraph().getNodes()) {
                map.put(node.getLabel(), node);
            }
            bfsList.add(map.get(srcNode.getLabel()));
            bfsBag.add(srcNode.getLabel());
            int bfsIndex = 0;
            boolean found = false;
            List<Integer> returnIndexes = new ArrayList<>();
            returnIndexes.add(-1);
            while ((bfsIndex < bfsList.size()) && !found) {
                String label = bfsList.get(bfsIndex).getLabel();
                for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                    if (link.canAccommodate(vl) && link.getSrcNode().equals(label) && !link.isLoop()) {
                        NetworkNode child = map.get(link.getDstNode());
                        if (!bfsBag.contains(child.getLabel())) {
                            bfsList.add(child);
                            bfsBag.add(child.getLabel());
                            returnIndexes.add(bfsIndex);
                        }
                        if (child.getLabel().equals(dstNode.getLabel())) {
                            found = true;
                            break;
                        }
                    }
                }
                bfsIndex++;
            }
            if (!found) {
                return new ArrayList<>();
            } else {
                List<NetworkLink> path = new ArrayList<>();
                int returnIndex = returnIndexes.get(returnIndexes.size() - 1);
                NetworkNode linkDst = bfsList.get(bfsList.size() - 1);
                while (returnIndex != -1) {
                    NetworkNode linkSrc = bfsList.get(returnIndex);
                    for (NetworkLink link : state.getNetworkGraph().getLinks()) {
                        if (link.getSrcNode().equals(linkSrc.getLabel()) && link.getDstNode().equals(linkDst.getLabel())) {
                            path.add(link);
                            break;
                        }
                    }
                    returnIndex = returnIndexes.get(returnIndex);
                    linkDst = linkSrc;
                }
                Collections.reverse(path);
                return path;
            }
        }
    }
}
