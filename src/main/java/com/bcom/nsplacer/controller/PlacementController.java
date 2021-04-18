package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class PlacementController {

    public static Placer placer;

    public static NetworkGraph networkGraph = createNetworkGraphSample();
    public static ServiceGraph serviceGraph = createServiceGraphSample();

    public static NetworkGraph createNetworkGraphSample() {
        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < 6; i++) {
            NetworkNode n = new NetworkNode();
            n.setLabel("N" + (i + 1));
            graph.getNodes().add(n);
        }
        for (int i = 0; i < 24; i++) {
            NetworkLink l = new NetworkLink();
            l.setLabel("PL" + (i + 1));
            graph.getLinks().add(l);
        }
        graph.getLinks().get(0).setSrcNode("N1");
        graph.getLinks().get(0).setDstNode("N2");
        graph.getLinks().get(1).setSrcNode("N2");
        graph.getLinks().get(1).setDstNode("N1");

        graph.getLinks().get(2).setSrcNode("N2");
        graph.getLinks().get(2).setDstNode("N3");
        graph.getLinks().get(3).setSrcNode("N3");
        graph.getLinks().get(3).setDstNode("N2");

        graph.getLinks().get(4).setSrcNode("N1");
        graph.getLinks().get(4).setDstNode("N4");
        graph.getLinks().get(5).setSrcNode("N4");
        graph.getLinks().get(5).setDstNode("N1");

        graph.getLinks().get(6).setSrcNode("N2");
        graph.getLinks().get(6).setDstNode("N5");
        graph.getLinks().get(7).setSrcNode("N5");
        graph.getLinks().get(7).setDstNode("N2");

        graph.getLinks().get(8).setSrcNode("N2");
        graph.getLinks().get(8).setDstNode("N6");
        graph.getLinks().get(9).setSrcNode("N6");
        graph.getLinks().get(9).setDstNode("N2");

        graph.getLinks().get(10).setSrcNode("N3");
        graph.getLinks().get(10).setDstNode("N5");
        graph.getLinks().get(11).setSrcNode("N5");
        graph.getLinks().get(11).setDstNode("N3");

        graph.getLinks().get(12).setSrcNode("N3");
        graph.getLinks().get(12).setDstNode("N6");
        graph.getLinks().get(13).setSrcNode("N6");
        graph.getLinks().get(13).setDstNode("N3");

        graph.getLinks().get(14).setSrcNode("N4");
        graph.getLinks().get(14).setDstNode("N5");
        graph.getLinks().get(15).setSrcNode("N5");
        graph.getLinks().get(15).setDstNode("N4");

        graph.getLinks().get(16).setSrcNode("N5");
        graph.getLinks().get(16).setDstNode("N6");
        graph.getLinks().get(17).setSrcNode("N6");
        graph.getLinks().get(17).setDstNode("N5");

        int j = 1;
        for (int i = 18; i < 24; i++) {
            graph.getLinks().get(i).setSrcNode("N" + j);
            graph.getLinks().get(i).setDstNode("N" + j);
            graph.getLinks().get(i).setMaximumResourceValue(ResourceType.Bandwidth, 100000);
            graph.getLinks().get(i).setRemainingResourceValue(ResourceType.Bandwidth, 100000);
            j++;
        }
        return graph;
    }

    public static ServiceGraph createServiceGraphSample() {
        ServiceGraph graph = new ServiceGraph();
        graph.setDataFlowSrcVNF("V1");
        for (int i = 0; i < 4; i++) {
            VNF v = new VNF();
            v.setLabel("V" + (i + 1));
            graph.getVnfs().add(v);
        }
        for (int i = 0; i < 8; i++) {
            VirtualLink l = new VirtualLink();
            l.setLabel("VL" + (i + 1));
            graph.getVirtualLinks().add(l);
        }
        graph.getVirtualLinks().get(0).setSrcVNF("V1");
        graph.getVirtualLinks().get(0).setDstVNF("V2");
        graph.getVirtualLinks().get(1).setSrcVNF("V2");
        graph.getVirtualLinks().get(1).setDstVNF("V1");

        graph.getVirtualLinks().get(2).setSrcVNF("V2");
        graph.getVirtualLinks().get(2).setDstVNF("V3");
        graph.getVirtualLinks().get(3).setSrcVNF("V3");
        graph.getVirtualLinks().get(3).setDstVNF("V2");

        graph.getVirtualLinks().get(4).setSrcVNF("V3");
        graph.getVirtualLinks().get(4).setDstVNF("V4");
        graph.getVirtualLinks().get(5).setSrcVNF("V4");
        graph.getVirtualLinks().get(5).setDstVNF("V3");

        graph.getVirtualLinks().get(6).setSrcVNF("V4");
        graph.getVirtualLinks().get(6).setDstVNF("V1");
        graph.getVirtualLinks().get(7).setSrcVNF("V1");
        graph.getVirtualLinks().get(7).setDstVNF("V4");
        return graph;
    }

    @GetMapping("/api/placement/networkgraph/get")
    public NetworkGraph getNetworkGraph(HttpServletRequest request, HttpServletResponse response) {
        return networkGraph;
    }

    @PostMapping("/api/placement/networkgraph/set")
    public HttpStatus setNetworkGraph(HttpServletRequest request, HttpServletResponse response, @RequestBody String ng) {
        try {
            networkGraph = (NetworkGraph) StreamUtils.fromJson(ng, NetworkGraph.class);
        } catch (JsonProcessingException e) {
            try {
                networkGraph = ImportExportManager.importFromXML(ng, 1000, 1000, 1000);
            } catch (IOException ex) {
                return HttpStatus.BAD_REQUEST;
            }
            return HttpStatus.OK;
        }
        return HttpStatus.OK;
    }

    @GetMapping("/api/placement/servicegraph/get")
    public ServiceGraph getServiceGraph(HttpServletRequest request, HttpServletResponse response) {
        return serviceGraph;
    }

    @PostMapping("/api/placement/servicegraph/set")
    public HttpStatus setServiceGraph(HttpServletRequest request, HttpServletResponse response, @RequestBody ServiceGraph sg) {
        serviceGraph = sg;
        return HttpStatus.OK;
    }

    @GetMapping("/api/placement/start")
    public HttpStatus startPlacement(HttpServletRequest request, HttpServletResponse response) {
        if (placer != null) {
            placer.stop();
        }
        placer = new Placer(networkGraph, serviceGraph, true, PlacerType.BestFound, RoutingType.HopCount,
                PlacerStrategy.SAP, 60000, null);
        new Thread(placer).start();
        return HttpStatus.OK;
    }

    @GetMapping("/api/placement/stop")
    public HttpStatus stopPlacement(HttpServletRequest request, HttpServletResponse response) {
        placer.stop();
        return HttpStatus.OK;
    }

    @GetMapping("/api/placement/status")
    public String statusPlacement(HttpServletRequest request, HttpServletResponse response) {
        return placer.getStatus();
    }
}
