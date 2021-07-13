package com.bcom.nsplacer.placement.evaluation;

import com.bcom.nsplacer.misc.InitializerParameters;
import com.bcom.nsplacer.misc.MathUtils;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.placement.*;
import com.bcom.nsplacer.placement.enums.*;
import com.bcom.nsplacer.placement.routing.DijkstraRoutingAlgorithm;
import com.bcom.nsplacer.placement.routing.RoutingAlgorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class Evaluation {

    public static int timeout = 4000;
    public static InitializerParameters requiredCpu = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredStorage = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredBandwidth = new InitializerParameters(null, 0, 0, 1);
    public static InitializerParameters requiredLatency = new InitializerParameters(null, 0, 0, 100);
    public static InitializerParameters availableCpu = new InitializerParameters(null, 0, 0, 1000000);
    public static InitializerParameters availableStorage = new InitializerParameters(null, 0, 0, 1000000);
    public static InitializerParameters availableBandwidth = new InitializerParameters(null, 0, 0, 10);
    public static InitializerParameters availableLatency = new InitializerParameters(null, 0, 0, 1);

    public static void singleTest() throws Exception {
        //String SNTopology = "./samples of network graphs/zoo-topologies/BtNorthAmerica.graphml.xml";
        String SNTopology = "./samples of network graphs/zoo-topologies/BtEurope.graphml.xml";
        NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(StreamUtils.readString(new File(SNTopology)), availableCpu, availableStorage, availableBandwidth, availableLatency);

        RoutingAlgorithm routingAlgorithm = new DijkstraRoutingAlgorithm(networkGraph);
        Placer placer = new Placer(networkGraph, null, true, false, false,
                PlacerType.FirstFound, ObjectiveType.Bandwidth, routingAlgorithm,
                SearchStrategy.ABO, timeout, new PlacerTerminationAction() {
            @Override
            public void perform(Placer placer) {
            }
        });

        int cnt = 0;
        ServiceGraph serviceGraph = new ServiceGraph();
        long totalTime = 0, totalLatency = 0;
        //IDSRoutingAlgorithm.FLOOR = 9;
        while (true) {
            serviceGraph.create(TopologyType.DaisyChain, 8, requiredCpu, requiredStorage, requiredBandwidth, requiredLatency);
            placer.setServiceGraph(serviceGraph);
            placer.run();
            if (placer.hasFoundPlacement()) {
                int latency = placer.getBestFoundState().calcPlacedLatency();
                System.out.println("Latency: " + latency);
                totalLatency += latency;

                totalTime += placer.getExecutionTime();
                placer.applyNetworkStateFromBestFoundPlacement();

                double totalRemainingBandwidth = placer.getNetworkGraph().getTotalRemainingResourceValue(false, ResourceType.Bandwidth);
                System.out.println("totalRemainingBandwidth: " + totalRemainingBandwidth);

                cnt++;
                System.out.println("# of placed services: " + cnt);
            } else {
                //if (IDSRoutingAlgorithm.FLOOR == 0) {
                break;
                //} else {
                //    IDSRoutingAlgorithm.FLOOR--;
                //}
            }
        }
        System.out.println("Total time: " + totalTime);
        System.out.println("Average latency: " + (int) ((double) totalLatency / cnt));
    }

    public static void evaluation() throws Exception {
        for (String SNTopology : Arrays.asList(
                "./samples of network graphs/zoo-topologies/BtEurope.graphml.xml"
                //,"./samples of network graphs/zoo-topologies/BtAsiaPac.graphml.xml"
                //,"./samples of network graphs/zoo-topologies/BtNorthAmerica.graphml.xml"
        )) {
            PlacerType placerType = PlacerType.FirstFound;
            System.out.println(SNTopology);

            NetworkGraph networkGraph = ZooTopologyImportExportManager.importFromXML(StreamUtils.readString(new File(SNTopology)), availableCpu, availableStorage, availableBandwidth, availableLatency);
            RoutingAlgorithm routingAlgorithm = new DijkstraRoutingAlgorithm(networkGraph);

            System.out.println("Demand\tServiceTopology\tMethod\tServiceSize\t#PlacedServices\tQ0\tQ1\tQ2\tQ3\tQ4\tAverage\tFinalRemainedBandwidthPercent\tAverageUsedBandwidthPercent");
            for (int bandwidthDemand = 1; bandwidthDemand <= requiredBandwidth.get(); bandwidthDemand++) {
                for (TopologyType topologyType : Arrays.asList(TopologyType.DaisyChain, TopologyType.Ring, TopologyType.Star)) {
                    for (SearchStrategy strategy : Arrays.asList(SearchStrategy.VOTE
                            , SearchStrategy.ABO
                            , SearchStrategy.DBO
                            //, SearchStrategy.EIFF
                            //, SearchStrategy.EDFF
                    )) {
                        for (int serviceSize = 3; serviceSize <= 8; serviceSize++) {
                            Placer placer = new Placer(networkGraph, null, true, false, false, placerType, ObjectiveType.Bandwidth, routingAlgorithm, strategy, timeout, null);

                            int cnt = 0;
                            List<Long> times = new ArrayList<>();
                            Random random = null;
                            ServiceGraph serviceGraph = new ServiceGraph();
                            while (true) {
                                serviceGraph.create(topologyType, serviceSize, requiredCpu, requiredStorage, requiredBandwidth, requiredLatency);
                                placer.setServiceGraph(serviceGraph);
                                placer.run();
                                if (placer.hasFoundPlacement()) {
                                    placer.applyNetworkStateFromBestFoundPlacement();
                                    cnt++;
                                    times.add(placer.getExecutionTime());
                                } else {
                                    String precision = "%.2f";
                                    double percentRemaining = ((double) placer.getNetworkGraph().getTotalRemainingResourceValue(false, ResourceType.Bandwidth) /
                                            placer.getNetworkGraph().getTotalMaximumResourceValue(false, ResourceType.Bandwidth) * 100.0);
                                    double usedResourcePerServicePercent = (100.0 - percentRemaining) / cnt;
                                    List<Long> quartiles = MathUtils.quartile(times);
                                    if (quartiles.isEmpty()) {
                                        for (int i = 0; i < 5; i++) {
                                            quartiles.add(0l);
                                        }
                                    }
                                    System.out.println(bandwidthDemand
                                            + "\t" + topologyType
                                            + "\t" + strategy
                                            + "\t" + serviceSize
                                            + "\t" + cnt
                                            + "\t" + quartiles.get(0)
                                            + "\t" + quartiles.get(1)
                                            + "\t" + quartiles.get(2)
                                            + "\t" + quartiles.get(3)
                                            + "\t" + quartiles.get(4)
                                            + "\t" + (int) MathUtils.genericAverage(times)
                                            + "\t" + String.format(precision, percentRemaining)
                                            + "\t" + String.format(precision, usedResourcePerServicePercent)
                                    );

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        singleTest();
        //evaluation();
        //compareTheResults();
    }

    public static void compareTheResults() throws Exception {

        String files[] = new String[]{
                //"C:\\Users\\mtaghavian\\Desktop\\Paper-globecom2021\\Evaluations\\BTEUROPE-RECURSION.xlsx"//,
                "C:\\Users\\mtaghavian\\Desktop\\Paper-globecom2021\\Evaluations\\BTEUROPE3.xlsx",
                "C:\\Users\\mtaghavian\\Desktop\\Paper-globecom2021\\Evaluations\\BTASIA3.xlsx",
                "C:\\Users\\mtaghavian\\Desktop\\Paper-globecom2021\\Evaluations\\BTAMERICA3.xlsx"
        };

        File vbsFile = new File("XlsToCsv.vbs");
        Map<String, List<Double>> map = new HashMap<>();
        map.put("" + SearchStrategy.VOTE, new ArrayList<>());
        map.put("" + SearchStrategy.ABO, new ArrayList<>());
        map.put("" + SearchStrategy.DBO, new ArrayList<>());
        map.put("" + SearchStrategy.EDFF, new ArrayList<>());
        map.put("" + SearchStrategy.EIFF, new ArrayList<>());

        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            File tempFile = new File(file.getName() + ".csv");
            if (!tempFile.exists()) {
                String output = runCommand("cscript " + vbsFile.getAbsolutePath() + " " + file.getAbsolutePath() + " " + tempFile.getAbsolutePath(), null);
            }
            System.out.println("Network: " + tempFile.getName());
            compareHelper(tempFile.getName(), tempFile, map);
        }

        for (String strategy : Arrays.asList("" + SearchStrategy.VOTE, "" + SearchStrategy.ABO, "" + SearchStrategy.DBO, "" + SearchStrategy.EDFF, "" + SearchStrategy.EIFF)) {
            System.out.println("Strategy: " + strategy);
            Collections.sort(map.get(strategy));
            List<Double> quartile = MathUtils.quartile(map.get(strategy));
            if (!quartile.isEmpty()) {
                for (int i = 0; i < quartile.size(); i++) {
                    System.out.print(toString(quartile.get(i), 2) + "\t");
                }
            }
            System.out.print(toString(MathUtils.average(map.get(strategy)), 2));
            System.out.println();
        }
    }

    public static void compareHelper(String substrateNetwork, File file, Map<String, List<Double>> map) throws Exception {
        Scanner sc = new Scanner(StreamUtils.readString(file));
        sc.nextLine();
        double[] echele = new double[6];
        int i = 0;
        String mode = "placedServiceCount";
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if ("".equals(line)) {
                continue;
            }
            String[] split = line.split(",");
            String topology = split[1];
            String strategy = split[2];
            int serviceSize = Integer.parseInt(split[3]);
            double placedServiceCount = Double.parseDouble(split[4]);
            double averageUsedBandwidth = Double.parseDouble(split[9]);
            //double averageTime = Double.parseDouble(split[10]);
            if (mode.equals("placedServiceCount")) {
                if (strategy.equals("" + SearchStrategy.VOTE)) {
                    echele[serviceSize - 3] = placedServiceCount;
                } else {
                    double r = echele[serviceSize - 3] / (placedServiceCount == 0.0 ? 1.0 : placedServiceCount);
                    if (r < 1.0) {
                        System.out.println(line);
                    }
                    map.get(strategy).add(r);
                }
            } else if (mode.equals("averageTime")) {
                //map.get(strategy).add(averageTime);
            } else if (mode.equals("averageUsedBandwidth")) {
                if (!Double.isNaN(averageUsedBandwidth)) {
                    double r = averageUsedBandwidth / ((topology.equals("Ring") ? serviceSize : (serviceSize - 1)) * 2);
                    map.get(strategy).add(r);
                }
            }
            i++;
        }
        sc.close();
    }

    public static String runCommand(String cmd, String systemInput) {
        Process p;
        int status = -1;
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        try {
            p = Runtime.getRuntime().exec(cmd);
            if (systemInput != null) {
                p.getOutputStream().write(systemInput.getBytes());
            }
            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public static String toString(double d, int precision) {
        return String.format("%." + precision + "f", d);
    }
}
