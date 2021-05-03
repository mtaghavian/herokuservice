package com.bcom.nsplacer.placement;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class Placer implements Runnable {

    public int totalCreatedStates, totalFeasibleStates;
    private NetworkGraph networkGraph;
    private ServiceGraph serviceGraph;
    private String status = "";
    private volatile boolean running = false;
    private SearchState bestFoundState;
    private int totalFeasiblePlacements = 0;
    private long beginTime, finishTime;
    private boolean recursive;
    private PlacerType placerType;
    private RoutingType routingType;
    private PlacerStrategy strategy;
    private long timeout;
    private PlacerTerminationAction placerTermination;
    private int combinedMethodState = 0;

    public Placer(NetworkGraph networkGraph, ServiceGraph serviceGraph, boolean recursive, PlacerType placerType, RoutingType routingType,
                  PlacerStrategy s, long timeout, PlacerTerminationAction action) {
        this.strategy = s;
        this.networkGraph = networkGraph.clone();
        this.recursive = recursive;
        this.placerType = placerType;
        this.routingType = routingType;
        this.timeout = timeout;
        this.placerTermination = action;
        setServiceGraph(serviceGraph);
        totalCreatedStates = totalFeasibleStates = 0;
    }

    public void setServiceGraph(ServiceGraph serviceGraph) {
        if (serviceGraph != null) {
            this.serviceGraph = serviceGraph.clone();
        }
    }

    private void runHelper(PlacerStrategy s, long t) {
        long checkpoint = beginTime;
        Fringe fringe = new Fringe(PlacerStrategy.ABO.equals(s));
        SearchState root = new SearchState(networkGraph, serviceGraph.traverse(), 0, this, s);
        fringe.put(Arrays.asList(root));
        while (!fringe.isEmpty() && running && ((System.currentTimeMillis() - beginTime) < t)) {
            SearchState state = fringe.take();
            if (state.isFeasible()) {
                if (state.isTerminal()) {
                    totalFeasiblePlacements++;
                    if ((bestFoundState == null) || (state.getOptimizerValue() > bestFoundState.getOptimizerValue())) {
                        bestFoundState = state;
                    }
                    if (PlacerType.FirstFound.equals(placerType)) {
                        break;
                    }
                } else {
                    List<SearchState> children = state.expand();
                    fringe.put(children);
                }
            }
            long checkpointFinishTime = System.currentTimeMillis();
            if ((checkpointFinishTime - checkpoint) > 500l) {
                updateStatus(false, bestFoundState, checkpointFinishTime, beginTime, totalFeasiblePlacements);
                checkpoint = checkpointFinishTime;
            }
        }
    }

    @Override
    public void run() {
        if (!running) {
            running = true;
            beginTime = System.currentTimeMillis();
            try {
                totalFeasiblePlacements = 0;
                bestFoundState = null;
                if (PlacerStrategy.ADBO.equals(getStrategy())) {
                    runHelper(PlacerStrategy.ABO, timeout);
                    if (!hasFoundPlacement()) {
                        runHelper(PlacerStrategy.DBO, timeout * 2);
                        if (!hasFoundPlacement()) {
                            runHelper(PlacerStrategy.EDFF, timeout * 3);
                        }
                    }
                } else if (PlacerStrategy.DFF.equals(getStrategy())) {
                    strategy = PlacerStrategy.EDFF;
                    recursive = false;
                    runHelper(getStrategy(), timeout);
                } else if (PlacerStrategy.IFF.equals(getStrategy())) {
                    strategy = PlacerStrategy.EIFF;
                    recursive = false;
                    runHelper(getStrategy(), timeout);
                } else {
                    runHelper(getStrategy(), timeout);
                }
            } finally {
                finishTime = System.currentTimeMillis();
                updateStatus(true, bestFoundState, finishTime, beginTime, totalFeasiblePlacements);
                running = false;
                if (placerTermination != null) {
                    placerTermination.perform(this);
                }
            }
        }
    }

    public synchronized void stop() {
        running = false;
    }

    public synchronized String getStatus() {
        return status;
    }

    public long getExecutionTime() {
        return finishTime - beginTime;
    }

    private void updateStatus(boolean isFinished, SearchState bestFoundState, long etime, long btime, int completePlacementCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search progress status: " + (isFinished ? "Finished successfully" : "Still continuing") + "\n");
        sb.append("Execution time: " + (etime - btime) + " ms" + "\n");
        sb.append("Total feasible placements found: " + completePlacementCount + "\n");
        sb.append("Total created states: " + totalCreatedStates + "\n");
        sb.append("Total feasible states: " + totalFeasibleStates + "\n");
        if (bestFoundState != null) {
            sb.append("Best placement found summary: {" + "\n");
            sb.append("  Placement for nodes: " + bestFoundState.getPlacementNodeMap() + "\n");
            sb.append("  Placement for links: " + bestFoundState.getPlacementLinkMap() + "\n");
            sb.append("  SAProbability: " + String.format("%.4f", bestFoundState.getOptimizerValue()) + "\n");
            sb.append("}" + "\n");
        }
        status = sb.toString();
    }

    public boolean hasFoundPlacement() {
        return (bestFoundState != null);
    }

    public void applyNetworkStateFromBestFoundPlacement() {
        networkGraph = bestFoundState.getNetworkGraph().clone();
    }
}
