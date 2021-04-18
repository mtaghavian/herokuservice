package com.bcom.nsplacer.placement;

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
public class VirtualLink {

    private String label;

    private String srcVNF, dstVNF;

    private List<Resource> requiredResources;

    public VirtualLink() {
        requiredResources = new LinkedList<>();
        requiredResources.add(new Resource(ResourceType.Bandwidth, 100));
        label = "unknown";
    }

    public int getRequiredResourceValue(ResourceType type) {
        for (Resource r : requiredResources) {
            if (type.equals(r.getType())) {
                return r.getValue();
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public void setRequiredResourceValue(ResourceType type, int v) {
        for (Resource r : requiredResources) {
            if (type.equals(r.getType())) {
                r.setValue(v);
                return;
            }
        }
        throw new RuntimeException("Resource of type = " + type + " not found!");
    }

    public VirtualLink clone() {
        VirtualLink l = new VirtualLink();
        l.setLabel(getLabel());
        l.setSrcVNF(getSrcVNF());
        l.setDstVNF(getDstVNF());
        l.setRequiredResourceValue(ResourceType.Bandwidth, getRequiredResourceValue(ResourceType.Bandwidth));
        return l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualLink that = (VirtualLink) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    public void setRandomValues(Random random, int maxBandwidthDemand) {
        setRequiredResourceValue(ResourceType.Bandwidth, Math.abs(random.nextInt()) % maxBandwidthDemand + 1);
    }
}
