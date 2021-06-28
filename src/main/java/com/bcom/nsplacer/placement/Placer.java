package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ObjectiveType;
import com.bcom.nsplacer.placement.enums.PlacerType;
import com.bcom.nsplacer.placement.enums.SearchStrategy;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

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
    private boolean backtracking, shuffle;
    private PlacerType placerType;
    private RoutingAlgorithm routingAlgorithm;
    private SearchStrategy strategy;
    private long timeout;
    private PlacerTerminationAction placerTermination;
    private ObjectiveType objectiveType;
    private boolean checkOverallLatency;

    public Placer(NetworkGraph networkGraph, ServiceGraph serviceGraph,
                  boolean backtracking, boolean shuffle, boolean checkOverallLatency,
                  PlacerType placerType,
                  ObjectiveType objectiveType,
                  RoutingAlgorithm routingAlgorithm,
                  SearchStrategy strategy,
                  long timeout, PlacerTerminationAction action) {
        this.strategy = strategy;
        this.networkGraph = networkGraph.clone();
        this.backtracking = backtracking;
        this.shuffle = shuffle;
        this.placerType = placerType;
        this.objectiveType = objectiveType;
        this.timeout = timeout;
        this.placerTermination = action;
        this.routingAlgorithm = routingAlgorithm;
        setServiceGraph(serviceGraph);
        totalCreatedStates = totalFeasibleStates = 0;
    }

    public void setServiceGraph(ServiceGraph g) {
        if (g != null) {
            this.serviceGraph = g.clone();
        }
    }

    private void runHelper(SearchStrategy st, long tout) {
        long checkpoint = beginTime;
        Fringe fringe = new Fringe(SearchStrategy.ABO.equals(st), shuffle);
        SearchState root = new SearchState(networkGraph, serviceGraph.traverse(), 0, this, st, checkOverallLatency);
        fringe.put(Arrays.asList(root));
        while (!fringe.isEmpty() && running && ((System.currentTimeMillis() - beginTime) < tout)) {
            SearchState state = fringe.take();
            if (state.isFeasible()) {
                if (state.isTerminal()) {
                    totalFeasiblePlacements++;
                    if ((bestFoundState == null) || (st.isMaximizer() ?
                            (state.getObjectiveValue() > bestFoundState.getObjectiveValue()) :
                            (state.getObjectiveValue() < bestFoundState.getObjectiveValue()))) {
                        bestFoundState = state;
                        //System.out.println("placement found! objective-value: " + bestFoundState.getObjectiveValue());
                    }
                    if (PlacerType.FirstFound.equals(placerType)) {
                        break;
                    }
                } else {
                    fringe.put(state.expand());
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
                if (SearchStrategy.VOTE.equals(getStrategy())) {
                    runHelper(SearchStrategy.ABO, timeout);
                    if (!hasFoundPlacement()) {
                        //System.out.println("Search strategy changed! " + SearchStrategy.ABO + " -> " + SearchStrategy.DBO);
                        runHelper(SearchStrategy.DBO, timeout * 2);
                        if (!hasFoundPlacement()) {
                            //System.out.println("Search strategy changed! " + SearchStrategy.DBO + " -> " + SearchStrategy.EDFF);
                            runHelper(SearchStrategy.EDFF, timeout * 3);
                        }
                    }
                } else if (SearchStrategy.DFF.equals(getStrategy())) {
                    strategy = SearchStrategy.EDFF;
                    backtracking = false;
                    runHelper(getStrategy(), timeout);
                } else if (SearchStrategy.IFF.equals(getStrategy())) {
                    strategy = SearchStrategy.EIFF;
                    backtracking = false;
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
            sb.append("  SAProbability: " + String.format("%.4f", bestFoundState.getObjectiveValue()) + "\n");
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
