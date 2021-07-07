package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.placement.NetworkLink;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoutingPath {

    private List<NetworkLink> links;

    public RoutingPath() {
        links = new ArrayList<>();
    }

    public RoutingPath(List<NetworkLink> links) {
        this.links = links;
    }
}
