package com.bcom.nsplacer.misc;

import com.bcom.nsplacer.placement.NetworkGraph;
import com.bcom.nsplacer.placement.NetworkLink;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Implementation of the Floyd-Warshall Algorithm.
 *
 * @author <a href="sven@happycoders.eu">Sven Woltmann</a>
 */
@NoArgsConstructor
public class FWRouting {

    private int maxCost;
    private int[][] costs;
    private int[][] successors;
    private int num;
    private String[] nodes;
    private Map<String, Integer> nodeNameToIndex;
    private NetworkGraph graph;

    public int getMaxDist() {
        return maxCost;
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    public void build(NetworkGraph graph) {
        this.graph = graph;
        int n = graph.getNodes().size();
        String[] nodes = graph.getNodes().stream().map(x -> x.getLabel()).toArray(String[]::new);

        this.num = nodes.length;
        this.nodes = nodes;

        // Create lookup map from node name to node index
        Map<String, Integer> temp = new HashMap<>();
        for (int i = 0; i < num; i++) {
            temp.put(nodes[i], i);
        }
        this.nodeNameToIndex = Collections.unmodifiableMap(temp);

        // Create cost and successor matrix
        this.costs = new int[num][num];
        this.successors = new int[num][num];

        // Preparation
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    costs[i][j] = 0;
                    successors[i][j] = -1;
                } else {
                    NetworkLink targetLink = null;
                    for (NetworkLink link : graph.getLinks()) {
                        if (link.getSrcNode().equals(nodes[i]) && link.getDstNode().equals(nodes[j])) {
                            targetLink = link;
                            break;
                        }
                    }
                    if (targetLink == null) {
                        costs[i][j] = Integer.MAX_VALUE;
                        successors[i][j] = -1;
                    } else {
                        costs[i][j] = 1;
                        successors[i][j] = j;
                    }
                }
            }
        }
        // Iterations
        maxCost = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int costViaNodeK = addCosts(costs[i][k], costs[k][j]);
                    if (costViaNodeK < costs[i][j]) {
                        costs[i][j] = costViaNodeK;
                        successors[i][j] = successors[i][k];
                    }
                    if (k == n - 1) {
                        maxCost = (costs[i][j] != Integer.MAX_VALUE) ? Math.max(maxCost, costs[i][j]) : maxCost;
                    }
                }
            }
        }
        // Detect negative cycles
        for (int i = 0; i < n; i++) {
            if (costs[i][i] < 0) {
                throw new IllegalArgumentException("Graph has a negative cycle");
            }
        }
    }

    private int addCosts(int a, int b) {
        if (a == Integer.MAX_VALUE || b == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return a + b;
    }

    public int getDist(String source, String dest) {
        return costs[nodeNameToIndex.get(source)][nodeNameToIndex.get(dest)];
    }

    public List<String> getPath(String source, String dest) {
        int i = nodeNameToIndex.get(source);
        int j = nodeNameToIndex.get(dest);

        if (successors[i][j] == -1) {
            return new ArrayList<>();
        }

        List<String> path = new ArrayList<>();
        path.add(nodes[i]);

        while (i != j) {
            i = successors[i][j];
            path.add(nodes[i]);
        }

        return path;
    }

    public void print() {
        printCosts();
        printSuccessors();
    }

    private void printCosts() {
        System.out.println("Costs:");
        printHeader();
        for (int rowNo = 0; rowNo < num; rowNo++) {
            System.out.printf("%5s", nodes[rowNo]);
            for (int colNo = 0; colNo < num; colNo++) {
                int cost = costs[rowNo][colNo];
                if (cost == Integer.MAX_VALUE) System.out.print("    ∞");
                else System.out.printf("%5d", cost);
            }
            System.out.println();
        }
    }

    private void printSuccessors() {
        System.out.println("Successors:");
        printHeader();
        for (int rowNo = 0; rowNo < num; rowNo++) {
            System.out.printf("%5s", nodes[rowNo]);
            for (int colNo = 0; colNo < num; colNo++) {
                int successor = successors[rowNo][colNo];
                String nextNode = successor != -1 ? nodes[successor] : "-";
                System.out.printf("%5s", nextNode);
            }
            System.out.println();
        }
    }

    private void printHeader() {
        System.out.print("     ");
        for (int colNo = 0; colNo < num; colNo++) {
            System.out.printf("%5s", nodes[colNo]);
        }
        System.out.println();
    }

}