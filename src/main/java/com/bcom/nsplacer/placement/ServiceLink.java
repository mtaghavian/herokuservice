package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class ServiceLink {

    private String label;

    private String srcVNF, dstVNF;

    private List<Resource> requiredResources;

    public ServiceLink() {
        requiredResources = new LinkedList<>();
        requiredResources.add(new Resource(ResourceType.Bandwidth, 100));
        requiredResources.add(new Resource(ResourceType.Latency, 100));
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

    public ServiceLink clone() {
        ServiceLink l = new ServiceLink();
        l.setLabel(getLabel());
        l.setSrcVNF(getSrcVNF());
        l.setDstVNF(getDstVNF());
        l.requiredResources = new ArrayList<>();
        for (Resource r : requiredResources) {
            l.requiredResources.add(r.clone());
            //l.setRequiredResourceValue(r.getType(), getRequiredResourceValue(r.getType()));
        }
        return l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLink that = (ServiceLink) o;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

}
