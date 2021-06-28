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
public class NetworkNode implements Comparable<NetworkNode> {

    private String label;

    private List<Resource> remainingResources, maximumResources;

    public NetworkNode() {
        maximumResources = new LinkedList<>();
        maximumResources.add(new Resource(ResourceType.Cpu, 1000));
        maximumResources.add(new Resource(ResourceType.Storage, 1000));
        remainingResources = new LinkedList<>();
        remainingResources.add(new Resource(ResourceType.Cpu, getMaximumResourceValue(ResourceType.Cpu)));
        remainingResources.add(new Resource(ResourceType.Storage, getMaximumResourceValue(ResourceType.Storage)));
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

    public NetworkNode clone() {
        NetworkNode n = new NetworkNode();
        n.setLabel(getLabel());
        for (Resource r : remainingResources) {
            n.setRemainingResourceValue(r.getType(), getRemainingResourceValue(r.getType()));
            n.setMaximumResourceValue(r.getType(), getMaximumResourceValue(r.getType()));
        }
        return n;
    }

    public boolean checkFeasibility() {
        for (Resource r : remainingResources) {
            if (getRemainingResourceValue(r.getType()) < 0) {
                return false;
            }
        }
        return true;
    }

    public void setRandomValues(Random random) {
        for (Resource r : remainingResources) {
            setRemainingResourceValue(r.getType(), Math.abs(random.nextInt()) % getMaximumResourceValue(r.getType()));
        }
    }

    public boolean canAccommodate(VNF f) {
        for (Resource r : remainingResources) {
            if (getRemainingResourceValue(r.getType()) < f.getRequiredResourceValue(r.getType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkNode that = (NetworkNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public int compareTo(NetworkNode o) {
        return getLabel().compareTo(o.getLabel());
    }
}
