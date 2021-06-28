package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@ToString
public class NetworkLink implements Comparable<NetworkLink> {

    private String label;

    private String srcNode, dstNode;

    private List<Resource> remainingResources, maximumResources;

    public NetworkLink() {
        maximumResources = new LinkedList<>();
        maximumResources.add(new Resource(ResourceType.Bandwidth, 1000));
        maximumResources.add(new Resource(ResourceType.Latency, 1000));
        remainingResources = new LinkedList<>();
        remainingResources.add(new Resource(ResourceType.Bandwidth, getMaximumResourceValue(ResourceType.Bandwidth)));
        remainingResources.add(new Resource(ResourceType.Latency, getMaximumResourceValue(ResourceType.Latency)));
        label = "unknown";
    }

    public int getRemainingResourceValue(ResourceType type) {
        for (Resource r : remainingResources) {
            if (type.equals(r.getType())) {
                return r.getValue();
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public void setRemainingResourceValue(ResourceType type, int v) {
        for (Resource r : remainingResources) {
            if (type.equals(r.getType())) {
                r.setValue(v);
                return;
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public int getMaximumResourceValue(ResourceType type) {
        for (Resource r : maximumResources) {
            if (type.equals(r.getType())) {
                return r.getValue();
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public void setMaximumResourceValue(ResourceType type, int v) {
        for (Resource r : maximumResources) {
            if (type.equals(r.getType())) {
                r.setValue(v);
                return;
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public NetworkLink clone() {
        NetworkLink l = new NetworkLink();
        l.setLabel(getLabel());
        l.setSrcNode(getSrcNode());
        l.setDstNode(getDstNode());
        for (Resource r : remainingResources) {
            l.setRemainingResourceValue(r.getType(), getRemainingResourceValue(r.getType()));
            l.setMaximumResourceValue(r.getType(), getMaximumResourceValue(r.getType()));
        }
        return l;
    }

    public void setRandomValues(Random random) {
        for (Resource r : remainingResources) {
            setRemainingResourceValue(r.getType(), Math.abs(random.nextInt()) % getMaximumResourceValue(r.getType()));
        }
    }

    public boolean isLoop() {
        return srcNode.equals(dstNode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkLink that = (NetworkLink) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public int compareTo(NetworkLink o) {
        return getLabel().compareTo(o.getLabel());
    }
}
