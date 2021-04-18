package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.FileEntry;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.service.FileEntryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eval")
public class EvaluationController {

    public static int maxCpuDemand = 1;
    public static int maxStorageDemand = 1;
    public static int maxBandwidthAvailable = 10;
    public static int nodeCpuAndStorageMaxResources = 1000000;

    @Autowired
    private FileEntryService fileEntryService;

    private Map<String, SessionParams> sessions = new HashMap<>();

    private SessionParams getSessionParams(HttpServletRequest request) {
        if (!sessions.containsKey(request.getSession().getId())) {
            sessions.put(request.getSession().getId(), new SessionParams());
        }
        return sessions.get(request.getSession().getId());
    }

    @PostMapping("/start")
    public String start(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, String> params) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        sessionParams.params = params;
        sessionParams.status = null;
        sessionParams.counter = 0;
        String networkTopology = params.get("networkTopology");
        String strategy = params.get("strategy");
        String placerType = params.get("placerType");
        String routing = params.get("routing");
        String timeout = params.get("timeout");
        List<FileEntry> fileEntry = fileEntryService.findByName(networkTopology);
        NetworkGraph networkGraph = ImportExportManager.importFromXML(StreamUtils.readString(fileEntryService.getInputStream(fileEntry.get(0).getId())),
                nodeCpuAndStorageMaxResources, nodeCpuAndStorageMaxResources, maxBandwidthAvailable);
        sessionParams.placer = new Placer(networkGraph, null, true, PlacerType.valueOf(placerType),
                RoutingType.valueOf(routing), PlacerStrategy.valueOf(strategy),
                Integer.parseInt(timeout), null);
        new Thread(new Evaluator(sessionParams)).start();
        return "ok";
    }

    @GetMapping("/status")
    public String start(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        if (sessionParams.status == null) {
            return "NotFinished\n" + sessionParams.counter;
        } else {
            return "Finished\n" + sessionParams.status;
        }
    }

    @GetMapping("/getDetails")
    public String getDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        return sessionParams.placementDetails;
    }

    @GetMapping("/getParams")
    public Map<String, String> getParams(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        return sessionParams.params;
    }

    @GetMapping("/stop")
    public String stop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        sessionParams.running = false;
        try {
            sessionParams.placer.stop();
        } catch (Exception ex) {
        }
        return "ok";
    }

    @Getter
    @Setter
    public static class SessionParams {
        private Placer placer;
        private String status = null, placementDetails = null;
        private volatile int counter = 0;
        private volatile boolean running = false;
        private Map<String, String> params = new HashMap<>();
    }

    public static class Evaluator implements Runnable {

        private SessionParams sessionParams;

        public Evaluator(SessionParams sessionParams) {
            this.sessionParams = sessionParams;
        }

        @Override
        public void run() {
            try {
                String serviceTopology = sessionParams.params.get("serviceTopology");
                String bandwidth = sessionParams.params.get("bandwidth");
                String serviceSize = sessionParams.params.get("serviceSize");
                String strategy = sessionParams.params.get("strategy");
                TopologyType topologyType = TopologyType.valueOf(serviceTopology);
                int maxBandwidthDemand = Integer.parseInt(bandwidth);
                sessionParams.running = true;
                List<Long> times = new ArrayList<>();
                ServiceGraph serviceGraph = new ServiceGraph();
                StringBuilder sb = new StringBuilder();
                while (sessionParams.running) {
                    serviceGraph.create(null, topologyType, Integer.parseInt(serviceSize), maxCpuDemand, maxStorageDemand, maxBandwidthDemand);
                    sessionParams.placer.setServiceGraph(serviceGraph);
                    sessionParams.placer.run();
                    if (sessionParams.placer.hasFoundPlacement()) {
                        sessionParams.placer.applyNetworkStateFromBestFoundPlacement();
                        sb.append("Placement #" + sessionParams.counter).append("\n");
                        sb.append("Nodes: " + sessionParams.placer.getBestFoundState().getPlacementNodeMap()).append("\n");
                        sb.append("Links: " + sessionParams.placer.getBestFoundState().getPlacementLinkMap()).append("\n");
                        sb.append("\n");
                        sessionParams.counter++;
                        times.add(sessionParams.placer.getExecutionTime());
                    } else {
                        sessionParams.placementDetails = sb.toString();
                        String precision = "%.2f";
                        double percentRemaining = ((double) sessionParams.placer.getNetworkGraph().getTotalRemainingResourceValue(false, ResourceType.Bandwidth) /
                                sessionParams.placer.getNetworkGraph().getTotalMaximumResourceValue(false, ResourceType.Bandwidth) * 100.0);
                        double usedResourcePerServicePercent = (100.0 - percentRemaining) / sessionParams.counter;
                        List<Long> quartiles = MathUtils.quartile(times);
                        if (quartiles.isEmpty()) {
                            for (int i = 0; i < 5; i++) {
                                quartiles.add(0l);
                            }
                        }
                        sessionParams.status = maxBandwidthDemand
                                + "\n" + topologyType
                                + "\n" + strategy
                                + "\n" + serviceSize
                                + "\n" + sessionParams.counter
                                + "\n" + quartiles.get(0)
                                + "\n" + quartiles.get(1)
                                + "\n" + quartiles.get(2)
                                + "\n" + quartiles.get(3)
                                + "\n" + quartiles.get(4)
                                + "\n" + (int) MathUtils.average(times)
                                + "\n" + String.format(precision, percentRemaining)
                                + "\n" + String.format(precision, usedResourcePerServicePercent);
                        break;
                    }
                }
            } finally {
                sessionParams.running = false;
            }
        }
    }
}
