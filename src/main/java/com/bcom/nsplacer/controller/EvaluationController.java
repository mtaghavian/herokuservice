package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.misc.InitializerParameters;
import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.FileEntry;
import com.bcom.nsplacer.model.dto.EvaluationParams;
import com.bcom.nsplacer.model.dto.EvaluationResults;
import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.Placer;
import com.bcom.nsplacer.placement.ServiceGraph;
import com.bcom.nsplacer.placement.ZooTopologyImportExportManager;
import com.bcom.nsplacer.placement.enums.*;
import com.bcom.nsplacer.placement.routing.DijkstraRoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.UCSRoutingAlgorithm;
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

    public static InitializerParameters requiredCpu = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredStorage = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredBandwidth = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredLatency = new InitializerParameters(null, 0, 0, 100);
    public static InitializerParameters availableCpu = new InitializerParameters(null, 0, 0, 1000000);
    public static InitializerParameters availableStorage = new InitializerParameters(null, 0, 0, 1000000);
    public static InitializerParameters availableBandwidth = new InitializerParameters(null, 0, 0, 10);
    public static InitializerParameters availableLatency = new InitializerParameters(null, 0, 0, 1);

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
    public String start(HttpServletRequest request, HttpServletResponse response, @RequestBody EvaluationParams params) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        sessionParams.params = params;
        sessionParams.getResults().setCounter(0);
        String networkTopology = params.getNetworkTopology();
        List<FileEntry> fileEntry = fileEntryService.findByName(networkTopology);
        NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(StreamUtils.readString(fileEntryService.getInputStream(fileEntry.get(0).getId())), availableCpu, availableStorage, availableBandwidth, availableLatency);
        sessionParams.placer = new Placer(networkGraph, null, true, false, false, PlacerType.FirstFound, ObjectiveType.Bandwidth,
                params.getRouting().equals(RoutingType.UCS) ? new UCSRoutingAlgorithm(networkGraph) : new DijkstraRoutingAlgorithm(networkGraph),
                params.getStrategy(), params.getTimeout(), null);
        new Thread(new Evaluator(sessionParams)).start();
        return "ok";
    }

    @GetMapping("/status")
    public EvaluationResults start(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return getSessionParams(request).getResults();
    }

    @GetMapping("/getDetails")
    public String getDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        return sessionParams.placementDetails;
    }

    @GetMapping("/stop")
    public String stop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final SessionParams sessionParams = getSessionParams(request);
        try {
            sessionParams.getResults().setStop(true);
            sessionParams.placer.stop();
        } catch (Exception ex) {
        }
        return "ok";
    }

    @Getter
    @Setter
    public static class SessionParams {
        private Placer placer;
        private String placementDetails = null;
        private EvaluationResults results = new EvaluationResults();
        private EvaluationParams params;
    }

    public static class Evaluator implements Runnable {

        private SessionParams sessionParams;

        public Evaluator(SessionParams sessionParams) {
            this.sessionParams = sessionParams;
        }

        @Override
        public void run() {
            try {
                TopologyType topologyType = sessionParams.params.getServiceTopology();
                int bandwidthDemand = sessionParams.params.getBandwidth();
                sessionParams.getResults().setRunning(true);
                sessionParams.getResults().setStop(false);
                List<Long> times = new ArrayList<>();
                ServiceGraph serviceGraph = new ServiceGraph();
                StringBuilder sb = new StringBuilder();
                while (!sessionParams.getResults().isStop()) {
                    serviceGraph.create(topologyType, sessionParams.params.getServiceSize(), requiredCpu, requiredStorage, new InitializerParameters(null, 0, 0, bandwidthDemand), requiredLatency);
                    sessionParams.placer.setServiceGraph(serviceGraph);
                    sessionParams.placer.run();
                    if (sessionParams.placer.hasFoundPlacement()) {
                        sessionParams.placer.applyNetworkStateFromBestFoundPlacement();
                        sb.append("Placement #" + sessionParams.getResults().getCounter()).append("\n");
                        sb.append("Nodes: " + sessionParams.placer.getBestFoundState().getPlacementNodeMap()).append("\n");
                        sb.append("Links: " + sessionParams.placer.getBestFoundState().getPlacementLinkMap()).append("\n");
                        sb.append("\n");
                        sessionParams.getResults().incrementCounter();
                        times.add(sessionParams.placer.getExecutionTime());
                    } else {
                        break;
                    }
                }
                sessionParams.placementDetails = sb.toString();
                String precision = "%.2f";
                double percentRemaining = ((double) sessionParams.placer.getNetworkGraph().getTotalRemainingResourceValue(false, ResourceType.Bandwidth) /
                        sessionParams.placer.getNetworkGraph().getTotalMaximumResourceValue(false, ResourceType.Bandwidth) * 100.0);
                double usedResourcePerServicePercent = (100.0 - percentRemaining) / sessionParams.getResults().getCounter();
                List<Long> quartiles = MathUtils.quartile(times);
                if (quartiles.isEmpty()) {
                    for (int i = 0; i < 5; i++) {
                        quartiles.add(0l);
                    }
                }
                sessionParams.getResults().setQ0Time(quartiles.get(0).intValue());
                sessionParams.getResults().setQ1Time(quartiles.get(1).intValue());
                sessionParams.getResults().setQ2Time(quartiles.get(2).intValue());
                sessionParams.getResults().setQ3Time(quartiles.get(3).intValue());
                sessionParams.getResults().setQ4Time(quartiles.get(4).intValue());
                sessionParams.getResults().setAvgTime((int) MathUtils.genericAverage(times));
                sessionParams.getResults().setBwRemaining(String.format(precision, percentRemaining));
                sessionParams.getResults().setBwUsedPerService(String.format(precision, usedResourcePerServicePercent));
            } finally {
                sessionParams.getResults().setRunning(false);
            }
        }
    }
}
