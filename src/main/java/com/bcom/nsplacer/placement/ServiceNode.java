package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
public class ServiceNode {

    private String label;

    private List<Resource> requiredResources;

    public ServiceNode() {
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
        ServiceNode that = (ServiceNode) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    public ServiceNode clone() {
        ServiceNode n = new ServiceNode();
        n.setLabel(getLabel());
        for (Resource r : requiredResources) {
            n.setRequiredResourceValue(r.getType(), getRequiredResourceValue(r.getType()));
        }
        return n;
    }
}
