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
public class VNF {

    private String label;

    private List<Resource> requiredResources;

    public VNF() {
        requiredResources = new LinkedList<>();
        requiredResources.add(new Resource(ResourceType.Cpu, 100));
        requiredResources.add(new Resource(ResourceType.Storage, 100));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VNF that = (VNF) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    public void setRandomValues(Random random, int maxCpuDemand, int maxStorageDemand) {
        setRequiredResourceValue(ResourceType.Cpu, Math.abs(random.nextInt()) % maxCpuDemand + 1);
        setRequiredResourceValue(ResourceType.Storage, Math.abs(random.nextInt()) % maxStorageDemand + 1);
    }

    public VNF clone() {
        VNF n = new VNF();
        n.setLabel(getLabel());
        for (Resource r : requiredResources) {
            n.setRequiredResourceValue(r.getType(), getRequiredResourceValue(r.getType()));
        }
        return n;
    }
}
