package com.bcom.nsplacer.placement.routing;

import com.bcom.nsplacer.placement.NetworkLink;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RoutingPath {

    private List<NetworkLink> links;

    public RoutingPath(List<NetworkLink> links) {
        this.links = links;
    }
}
