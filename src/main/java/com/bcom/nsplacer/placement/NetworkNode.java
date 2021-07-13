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

    private List<Resource> currentResources, maximumResources;

    public NetworkNode() {
        maximumResources = new LinkedList<>();
        maximumResources.add(new Resource(ResourceType.Cpu, 1000));
        maximumResources.add(new Resource(ResourceType.Storage, 1000));
        currentResources = new LinkedList<>();
        currentResources.add(new Resource(ResourceType.Cpu, getMaximumResourceValue(ResourceType.Cpu)));
        currentResources.add(new Resource(ResourceType.Storage, getMaximumResourceValue(ResourceType.Storage)));
        label = "unknown";
    }

    public int getCurrentResourceValue(ResourceType type) {
        for (Resource r : currentResources) {
            if (type.equals(r.getType())) {
                return r.getValue();
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public void setCurrentResourceValue(ResourceType type, int v) {
        for (Resource r : currentResources) {
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
        for (Resource r : currentResources) {
            n.setCurrentResourceValue(r.getType(), getCurrentResourceValue(r.getType()));
            n.setMaximumResourceValue(r.getType(), getMaximumResourceValue(r.getType()));
        }
        return n;
    }

    public boolean checkFeasibility() {
        for (Resource r : currentResources) {
            if (getCurrentResourceValue(r.getType()) < 0) {
                return false;
            }
        }
        return true;
    }

    public void setRandomValues(Random random) {
        for (Resource r : currentResources) {
            setCurrentResourceValue(r.getType(), Math.abs(random.nextInt()) % getMaximumResourceValue(r.getType()));
        }
    }

    public boolean canAccommodate(ServiceNode f) {
        for (Resource r : currentResources) {
            if (getCurrentResourceValue(r.getType()) < f.getRequiredResourceValue(r.getType())) {
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
