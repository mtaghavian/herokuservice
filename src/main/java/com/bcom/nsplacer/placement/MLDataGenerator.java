package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.misc.FileUtils;
import com.bcom.nsplacer.misc.SimpleCounter;
import com.bcom.nsplacer.misc.StreamUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MLDataGenerator {

    public static double performMonteCarlo(Random random, NetworkGraph networkGraph, int serviceSize,
                                           int experimentCount, int maxCpuDemand, int maxStorageDemand,
                                           int maxBandwidthDemand, int placementTimeout, boolean considerLinks) throws Exception {
        SimpleCounter counter = new SimpleCounter();
        boolean createLogFile = false;
        FileOutputStream fos = createLogFile ? new FileOutputStream(
                "mcarlo-" + serviceSize + "-" + UUID.randomUUID() + ".csv") : null;

        // Create a random service graph
        ServiceGraph serviceGraph = new ServiceGraph();
        serviceGraph.setDataFlowSrcVNF("V1");
        for (int i = 0; i < serviceSize; i++) {
            VNF v = new VNF();
            v.setLabel("V" + (i + 1));
            serviceGraph.getVnfs().add(v);

            if (i != 0) {
                if (considerLinks) {
                    VirtualLink l = new VirtualLink();
                    l.setLabel("VL" + i);
                    l.setSrcVNF("V" + i);
                    l.setDstVNF("V" + (i + 1));
                    serviceGraph.getVirtualLinks().add(l);
                }
            }
        }

        List<Double> list = new ArrayList<>();
        int i = 0;
        while (true) {
            i++;
            // set random demands for VNFs of the service
            for (VNF vnf : serviceGraph.getVnfs()) {
                vnf.setRandomValues(random, maxCpuDemand, maxStorageDemand);
            }
            for (VirtualLink vl : serviceGraph.getVirtualLinks()) {
                vl.setRandomValues(random, maxBandwidthDemand);
            }
            Placer placer = new Placer(networkGraph, serviceGraph, true, PlacerType.FirstFound, RoutingType.HopCount,
                    PlacerStrategy.EIFF, placementTimeout, new PlacerTerminationAction() {
                @Override
                public void perform(Placer placer) {
                    if (placer.hasFoundPlacement()) {
                        counter.increment();
                    }
                }
            });
            placer.run();
            double pr = ((double) counter.getCnt() / i);
            if (createLogFile) {
                fos.write(("" + pr + "\n").replace('.', ',').getBytes());
            }

            // Check the termination condition of our experiments
            list.add(pr);
            if (list.size() >= experimentCount) {
                boolean quit = true;
                double last = list.get(list.size() - 1);
                for (int j = 0; j < list.size(); j++) {
                    if (Math.abs(list.get(j) - last) > 0.01) {
                        quit = false;
                        break;
                    }
                }
                if (quit) {
                    break;
                } else {
                    list.clear();
                }
            }
        }
        if (createLogFile) {
            fos.close();
        }
        return (double) counter.getCnt() / i;
    }

    public static void generateDate(NetworkGraph networkGraph, int maxCpuDemand, int maxStorageDemand, int maxBandwidthDemand, int placementTimeout,
                                    int markovExperimentCount, int minServiceCount, int maxServiceCount, int datasetSize, int threadCount,
                                    boolean considerLinks) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        int datasetSizePerThread = datasetSize / threadCount;
        String pytorchFilename = "data-pytorch.csv";
        String excelFilename = "data-excel.csv";
        File dirFile = new File("generated-data");
        dirFile.mkdir();
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final NetworkGraph localNetworkGraph = networkGraph.clone();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    FileOutputStream pytorchCsvStream = null, excelCsvStream = null;
                    try {
                        pytorchCsvStream = new FileOutputStream(
                                new File("" + dirFile.getName() + "/" + threadIndex + "-" + pytorchFilename));
                        excelCsvStream = new FileOutputStream(
                                new File("" + dirFile.getName() + "/" + threadIndex + "-" + excelFilename));
                        Random random = new Random(System.currentTimeMillis());
                        for (int i = 0; i < datasetSizePerThread; i++) {

                            setRandomStateForNetworkGraph(random, localNetworkGraph);

                            double sap = 0.0;
                            for (int serviceSize = minServiceCount; serviceSize <= maxServiceCount; serviceSize++) {
                                // The Law of Total Probability
                                // We assume the probability of arriving services with different sizes are the same
                                sap += (1.0 / (maxServiceCount - minServiceCount + 1)) * performMonteCarlo(random, localNetworkGraph,
                                        serviceSize, markovExperimentCount, maxCpuDemand, maxStorageDemand, maxBandwidthDemand, placementTimeout, considerLinks);
                            }

                            // Saving a sample of the dataset
                            writeToStream(excelCsvStream, localNetworkGraph, sap, true);
                            writeToStream(pytorchCsvStream, localNetworkGraph, sap, false);

                            System.out.println("Thread = " + (threadIndex + 1) + "/" + threadCount +
                                    ", Datapoint = " + (i + 1) + "/" + datasetSizePerThread);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            excelCsvStream.close();
                        } catch (IOException ex) {
                        }
                        try {
                            pytorchCsvStream.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            });
        }

        // Wait until executor service shuts down
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);

        // Aggregating data files
        FileOutputStream pytorchCsvStream = new FileOutputStream(new File(pytorchFilename));
        FileOutputStream excelCsvStream = new FileOutputStream(new File(excelFilename));

        for (int i = 0; i < threadCount; i++) {
            File pytorchFilePart = new File("" + dirFile.getName() + "/" + i + "-" + pytorchFilename);
            File excelFilePart = new File("" + dirFile.getName() + "/" + i + "-" + excelFilename);

            StreamUtils.copy(new FileInputStream(pytorchFilePart), pytorchCsvStream, true, false);
            StreamUtils.copy(new FileInputStream(excelFilePart), excelCsvStream, true, false);
        }
        FileUtils.deleteDirectory(dirFile);
        pytorchCsvStream.close();
        excelCsvStream.close();
    }

    public static void generateData() throws Exception {
        NetworkGraph networkGraph = ImportExportManager.importFromXML(
                StreamUtils.readString(new File("./samples of network graphs/zoo-topologies/BtEurope.graphml.xml")), 1000, 1000, 1000);

        // Parameters
        int maxCpuDemand = 1000;
        int maxStorageDemand = 1000;
        int maxBandwidthDemand = 1000;
        int placementTimeout = 500;
        int markovExperimentCount = 500;
        int minServiceCount = 5;
        int maxServiceCount = 5;
        int datasetSize = 1000;
        int threadCount = 4;
        boolean considerLinks = false;

        long beginTime = System.currentTimeMillis();
        try {
            generateDate(networkGraph, maxCpuDemand, maxStorageDemand, maxBandwidthDemand, placementTimeout, markovExperimentCount,
                    minServiceCount, maxServiceCount, datasetSize, threadCount, considerLinks);
        } finally {
            long endTime = System.currentTimeMillis();
            System.out.println("Data generation finished. Execution time: " + ((endTime - beginTime) / 1000.0) + " s");
        }
    }

    public static void modifyData() throws Exception {
        FileOutputStream fos = new FileOutputStream(new File("data-pytorch-out.csv"));
        Scanner sc = new Scanner(StreamUtils.readString(new File("data-pytorch.csv")));
        while (sc.hasNext()) {
            String line = sc.nextLine().trim();
            if (line.equals("")) {
                continue;
            }
            String[] split = line.split(";");
            double sap = Double.parseDouble(split[0]);
            List<Integer> list = new ArrayList<>();
            for (int i = 1; i < split.length; i++) {
                list.add(Integer.parseInt(split[i]));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(sap);

            List<SortHelper> outList = new ArrayList<>();
            for (int i = 0; i < list.size(); i += 3) {
                SortHelper helper = new SortHelper();
                helper.list.add(list.get(i));
                helper.list.add(list.get(i + 1));
                helper.list.add(list.get(i + 2));
                outList.add(helper);
            }

            Collections.sort(outList);
            for (int i = 0; i < outList.size(); i++) {
                sb.append(";").append(outList.get(i).list.get(0));
                sb.append(";").append(outList.get(i).list.get(1));
                //sb.append(";").append(outList.get(i).list.get(2));
            }

            Collections.sort(outList, new Comparator<SortHelper>() {
                @Override
                public int compare(SortHelper o1, SortHelper o2) {
                    return o1.list.get(2).compareTo(o2.list.get(2));
                }
            });
            for (int i = 0; i < outList.size(); i++) {
                sb.append(";").append(outList.get(i).list.get(2));
            }

            sb.append("\n");
            fos.write(sb.toString().getBytes());
        }
        fos.close();
    }

    public static void main(String[] args) throws Exception {
        //generateData();
        modifyData();
    }

    private static void setRandomStateForNetworkGraph(Random random, NetworkGraph networkGraph) {
        for (NetworkLink link : networkGraph.getLinks()) {
            link.setRandomValues(random);
        }
        for (NetworkNode node : networkGraph.getNodes()) {
            node.setRandomValues(random);
        }
    }

    private static void writeToStream(OutputStream os, NetworkGraph networkGraph, double sap, boolean forExcel) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(forExcel ? ("" + sap).replace('.', ',') : ("" + sap));

        Map<String, Integer> map = networkGraph.mapOfAggregatedRemainingBandwidth();
        Collections.sort(networkGraph.getNodes());
        for (NetworkNode m : networkGraph.getNodes()) {
            sb.append(";").append(m.getRemainingResourceValue(ResourceType.Cpu));
            sb.append(";").append(m.getRemainingResourceValue(ResourceType.Storage));
            //sb.append(";").append(map.get(m.getLabel()));
        }
        sb.append("\n");
        os.write(sb.toString().getBytes());
    }

    public static class SortHelper implements Comparable<SortHelper> {
        public List<Integer> list = new ArrayList<>();
        public Integer value = null;

        @Override
        public int compareTo(SortHelper o) {
            if (value != null) {
                return value.compareTo(o.value);
            } else {
                double sum = 0;
                for (int i = 0; i < 2; i++) {
                    sum += list.get(i) - o.list.get(i);
                }
                return (int) Math.round(Math.signum(sum));
            }
        }
    }
}
