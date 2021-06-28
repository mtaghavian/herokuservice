package com.bcom.nsplacer.model.dto;

import com.bcom.nsplacer.placement.enums.RoutingType;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.enums.TopologyType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvaluationParams {

    private SearchStrategy strategy;
    private RoutingType routing;
    private TopologyType serviceTopology;
    private String networkTopology;
    private Integer bandwidth;
    private Integer timeout;
    private Integer serviceSize;

}
