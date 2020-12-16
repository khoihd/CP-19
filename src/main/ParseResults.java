package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;


public class ParseResults {

  public static final int FIRST_INSTANCE = 0;
  
  public static final int LAST_INSTANCE = 9;
    
//  public static final int[] AGENTS = {5, 10, 15, 20};
  public static final int[] AGENTS = {15};
  
  public static final int[] TIMEOUTS = {100};
//  public static final int[] TIMEOUTS = {100, 80, 60, 40, 20};
//    public static final int[] TIMEOUTS = {10, 20, 30, 40, 50, 60, 70, 80};

  
//  public static final double[] RATES = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
  
//  public static final double[] RATES = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
  
  public static final double[] RATES = {0.4};
  
  public static final String ALGORITHM = "RDIFF";
//  public static final String ALGORITHM = "RC-DIFF";
  
  public static final int NUMBER_OF_INSTANCES = 10;
  
//  public static final String GRAPH = "scale-free-tree";
  public static final String GRAPH = "random-network";
    
  public static final SortedMap<String, Double> solutionResult = new TreeMap<>();
    
  /**
   * Count map is wrong: 16 with count = 1 is difference with 8=1 and 8=1
   */
  public static final SortedMap<String, Double> countMap = new TreeMap<>();
  
  public static final SortedMap<Integer, Map<String, Double>> demandOverTime = new TreeMap<>();
  
  public void updateDemandOverTime(SortedMap<Integer, Map<String, Double>> demand, int run, String client, double demandValue) {
    Map<String, Double> clientDemand = demand.getOrDefault(run, new HashMap<>());
    clientDemand.merge(client, demandValue, Double::sum);
    demand.put(run, clientDemand);
  }
  
  public static void main(String[] args) {
    for (int agent : AGENTS) {
      solutionResult.clear();
      countMap.clear();
      
      for (int instance = FIRST_INSTANCE; instance < LAST_INSTANCE; instance++) {
        String scenarioPath = ALGORITHM + "/scenario/" + GRAPH + "/d" + agent + "/" + instance + "/";
        String instanceFile = scenarioPath + "/output/map.log";
        System.out.println(instanceFile);
        DijkstraShortestPath<String, String> pathFinder = getPathFinder(Paths.get(scenarioPath));
        readSolutionQuality(instanceFile, pathFinder);
      }
      
      for (Entry<String, Double> entry : solutionResult.entrySet()) {

//        entry.setValue(entry.getValue() / countMap.get(entry.getKey()));
        entry.setValue(entry.getValue() / NUMBER_OF_INSTANCES / 9);
//        entry.setValue(entry.getValue() / countMap.get(entry.getKey()) / NUMBER_OF_INSTANCES);
      }

      System.out.println("Number of agents: " + agent);
      System.out.println(solutionResult);
      System.out.println(solutionResult.values().stream().mapToDouble(Double::doubleValue).sum());
    }
  }
  
  public static void main_old(String[] args) {
    for (int agent : AGENTS) {
      solutionResult.clear();
      countMap.clear();
      for (double rate : RATES) {      
        String directory = "rate=" + rate + "/" + GRAPH;
           
        for (int instance = 0; instance < 10; instance++) {
//          String instanceFile = directory + "/" + instance + "/" + instance + ".log";
          String instanceFile = directory + "/" + instance + "/output/map.log";
          String scenarioPath = directory + "/" + instance;
          
          System.out.println(scenarioPath);
          System.out.println(instanceFile);
          
          DijkstraShortestPath<String, String> pathFinder = getPathFinder(Paths.get(scenarioPath));
          readSolutionQuality(instanceFile, pathFinder);
        }
        
        for (Entry<String, Double> entry : solutionResult.entrySet()) {
            entry.setValue(entry.getValue() / countMap.get(entry.getKey()));
        }
        
        System.out.println("Number of agents: " + agent);
        
        System.out.println(solutionResult);
        System.out.println(solutionResult.values().stream().mapToDouble(Double::doubleValue).sum());
      }
    }
  }
  
  
  private static void writeFile(String filename, String content) {
    PrintWriter writer;
    try {
      writer = new PrintWriter(filename, "UTF-8");
      writer.print(content);
      writer.close();
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
  }


  private static double[] readRuntimes(String file, int agent) {    
    Map<String, Double> currentStartTime = new HashMap<>();
    Map<String, Integer> numberOfCycles = new HashMap<>();
    Map<String, Double> agentRuntimeMap = new HashMap<>();
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      
      Pattern pattern = Pattern.compile("DCOP-node[A-Z]");
                
      while (line != null && !line.isEmpty()) {                        
        if (!line.contains("DCOP Run 1")) {
          line = br.readLine();
          continue;
        }
                                            
        // Start timing 
        if (line.contains("receives and processes message")) {
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
            String region = matcher.group().replaceAll("DCOP-node", "");
            if (Double.compare(currentStartTime.getOrDefault(region, 0D), 0D) == 0) {
              currentStartTime.put(region, Double.parseDouble(line.split(" ")[0]));
              numberOfCycles.put(region, numberOfCycles.getOrDefault(region, 0) + 1);
            }
          }
        } 
        else if (line.contains("end the current cycle")) {
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
            String region = matcher.group().replaceAll("DCOP-node", "");
            if (Double.compare(currentStartTime.getOrDefault(region, 0D), 0D) != 0) {
              long endTime = Long.parseLong(line.split(" ")[0]);
              agentRuntimeMap.put(region, (endTime - currentStartTime.get(region)) / numberOfCycles.get(region));
              currentStartTime.put(region, 0D);
            }
          }
        }
        
        line = br.readLine();
      }
      
      br.close();
    } catch (IOException e) {
      System.out.println(e);
    }    
    
    OptionalDouble averageDouble = agentRuntimeMap.values().stream().mapToDouble(Double::doubleValue).average();
    OptionalDouble minDouble = agentRuntimeMap.values().stream().mapToDouble(Double::doubleValue).min();
    OptionalDouble maxDouble = agentRuntimeMap.values().stream().mapToDouble(Double::doubleValue).max();
    
    double average = averageDouble.isPresent() ? averageDouble.getAsDouble() / agent : 0D;
    double min = minDouble.isPresent() ? minDouble.getAsDouble() / agent : 0D;
    double max = maxDouble.isPresent() ? maxDouble.getAsDouble() / agent : 0D;
    
    double[] result = {average, min, max};
    
    return result;
  }
  
  public static void main_1(String[] args) {
    DecimalFormat df = new DecimalFormat("#.##");
    df.setRoundingMode(RoundingMode.CEILING);
    
    for (int agent : AGENTS) {
      String qualityResult = ",";
      String averageRuntimeResult = ",";
      String minRuntimeResult = ",";
      String maxRuntimeResult = ",";
      
      for (double rate : RATES) {
        qualityResult += rate + ",";
        averageRuntimeResult += rate + ",";
        maxRuntimeResult += rate + ",";
        minRuntimeResult += rate + ",";
      }
      
      qualityResult += "\n";
      averageRuntimeResult += "\n";
      maxRuntimeResult += "\n";
      minRuntimeResult += "\n";

      for (int timeout : TIMEOUTS) {
        qualityResult += timeout + ",";
        averageRuntimeResult += timeout + ",";
        minRuntimeResult += timeout + ",";
        maxRuntimeResult += timeout + ",";

        for (double rate : RATES) {        
          double quality = 0D;
          double averageRuntime = 0D;
          double minRuntime = 0D;
          double maxRuntime = 0D;

          System.out.println("timeout=" + timeout + "/rate=" + rate);
          
          for (int instance = FIRST_INSTANCE; instance <= LAST_INSTANCE; instance++) {
            solutionResult.clear();
            countMap.clear();
            
            String directory = GRAPH + "/timeout=" + timeout + "/rate=" + rate + "/d" + agent + "/" + instance;
//            String file = directory + "/" + instance + ".log";
            String file = directory + "/output/map.log";
            double instanceQuality = readSolutionQuality(file, null);
            System.out.println("Instance = " + instance + ": Quality= " + instanceQuality);
            quality += instanceQuality;
            double[] runtimes = readRuntimes(file, agent);
            
            averageRuntime += runtimes[0];
            minRuntime += runtimes[1];
            maxRuntime += runtimes[2];
          }          
                    
          qualityResult += df.format(quality) + ",";
          averageRuntimeResult += df.format(averageRuntime) + ",";
          minRuntimeResult += df.format(minRuntime) + ",";
          maxRuntimeResult += df.format(maxRuntime) + ",";

        }
        
        qualityResult += "\n";
        averageRuntimeResult += "\n";
        minRuntimeResult += "\n";
        maxRuntimeResult += "\n";
      }
      
      System.out.println(qualityResult);
      System.out.println(averageRuntimeResult);
      System.out.println(minRuntimeResult);
      System.out.println(maxRuntimeResult);

      writeFile("quality.txt", qualityResult);
      writeFile("averageRuntime.txt", averageRuntimeResult);
      writeFile("minRuntime.txt", minRuntimeResult);
      writeFile("maxRuntime.txt", maxRuntimeResult);
    }
  
  }


  // 80604 [DCOP-nodeA] INFO com.bbn.map.dcop.cdiff.CdiffAlgorithm [] {}- AFTER DCOP FOR LOOP
  // , Iteration 19 Region A has getClientLoadMap {D={AppCoordinates {com.bbn, test-service3, 1}=0.49166666666666653
  // , AppCoordinates {com.bbn, test-service2, 1}=0.3687499999999999
  // , AppCoordinates {com.bbn, test-service1, 1}=0.5462962962962961}
  // , E={AppCoordinates {com.bbn, test-service3, 1}=0.49166666666666653
  // , AppCoordinates {com.bbn, test-service2, 1}=0.6145833333333331
  // , AppCoordinates {com.bbn, test-service1, 1}=0.437037037037037}}
  private static double readSolutionQuality(String file, DijkstraShortestPath<String, String> pathFinder) {    
    Map<String, Map<String, Double>> regionServiceQualityMap = new HashMap<>();
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));

      String line = br.readLine();
      while (line != null && !line.isEmpty()) {
//        if (ALGORITHM.equals("DPOP")) {
//          if (line.contains("AFTER DCOP FOR LOOP") && line.contains("getClientLoadMap")) {
//            dpopValue += Double.parseDouble(line.split("getClientLoadMap ")[1]);
//            count ++;
//            line = br.readLine();
//            continue;
//          }
//        }
        
        if (line.contains("getClientLoadMap") && line.contains("AppCoordinates")) {
          line = line.replaceAll("AFTER DCOP FOR LOOP, ", "");
          
          // Split the line into two halves
          String[] halves = line.split(" getClientLoadMap \\{");
          // 80604 [DCOP-nodeA]
          String region = halves[0].split(" ")[1].replaceAll("\\[DCOP-node", "").replaceAll("]", "");
          String[] splits = halves[1].split("=\\{AppCoordinates ");
          
//          System.out.println(halves[0].split(" ")[8]);
          
          Map<String, Double> serviceLoad = new HashMap<>();

          String client = "";
          for (int k = 0; k < splits.length; k++) {
            if (k == 0) {
              client = splits[k];
            }
            // Reading the service and load value
            // {com.bbn, test-service3, 1}=0.49166666666666653, AppCoordinates {com.bbn, test-service2, 1}=0.3687499999999999, AppCoordinates {com.bbn, test-service1, 1}=0.5462962962962961}, E
            // {com.bbn, test-service3, 1}=0.49166666666666653, AppCoordinates {com.bbn, test-service2, 1}=0.6145833333333331, AppCoordinates {com.bbn, test-service1, 1}=0.437037037037037}}
            else {
              String subline = splits[k];
              subline = subline.trim();
              if (subline.endsWith("\\{")) {
                subline = subline.substring(0, subline.length() - 1);
              } 
              subline = subline.replaceAll("com.bbn, test-service", "");
              subline = subline.replaceAll(", 1", "");
              subline = subline.replaceAll(" AppCoordinates ", "");
              subline = subline.replaceAll(" ", "");
              subline = subline.replaceAll("\\}", "");
              String[] sublineSplit = subline.split(",");
              for (String sub : sublineSplit) {
                if (sub.length() == 1) {
                  client = sub;
                } else {
                  String service = sub.split("=")[0].substring(1, 2);
                  double load = 0; 
                  try {
                    load = Double.parseDouble(sub.split("=")[1]);
                  } catch(ArrayIndexOutOfBoundsException e) {
                    continue;
                  }
//                  increaseDoubleValue(solutionResult, service, load / (pathFinder.getDistance(region, client).intValue() + 1));
//                  increaseDoubleValue(solutionResult, service, load); 
//                  serviceLoad.merge(service, load, Double::sum);
                  if (isPositive(load)) {
                    if (Double.compare(load, 17) > 0) {
                      load = 6.4;
                    }
                    
                    solutionResult.merge(service, load / (pathFinder.getDistance(region, client).intValue() + 1), Double::sum);
                    countMap.merge(service, 1D, Double::sum);
                  }
                }
              }
            }
          }
          
          regionServiceQualityMap.put(region, serviceLoad);
        }
            
        line = br.readLine();
      }
      
      br.close();
    } catch (IOException e) {
      System.out.println(e);
    }
    
//    for (Entry<String, Double> entry : solutionResult.entrySet()) {
//      entry.setValue(entry.getValue() / countMap.get(entry.getKey()) / NO_AGENT);
//    }
    
      double quality = 0;
      for (Entry<String, Map<String, Double>> entry : regionServiceQualityMap.entrySet()) {
        quality += entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
      }
      return quality;
  }
  
  private static boolean isPositive(double val) {
    return compareDouble(val, 0D) > 0;
  }
  
  public static int compareDouble(double a, double b) {
    double absDiff = Math.abs(a - b);
    
    if (Double.compare(absDiff, 1E-6) <= 0) return 0;
    
    return Double.compare(a, b);
}
  
//  private static void increaseCounter(SortedMap<Integer, SortedMap<String, Integer>> result, int agent, String service) {
//    SortedMap<String, Integer> serviceLoadMap = result.getOrDefault(agent, new TreeMap<>());
//    int counter = serviceLoadMap.getOrDefault(service, 0);
//    counter++;
//    serviceLoadMap.put(service, counter);
//    result.put(agent, serviceLoadMap);    
//  }

//  public static void increaseDoubleValue(SortedMap<String, Double> result, String service, double load) {
//    double serviceLoad = result.getOrDefault(service, 0.0);
//    serviceLoad += load;
//    result.put(service, serviceLoad);
//  }

  private static DijkstraShortestPath<String, String> getPathFinder(Path scenarioPath) {
	Graph<String, String> graph = new UndirectedSparseGraph<>();
    
    File topology = null;

    // Read the topology.ns
    File folder = scenarioPath.toFile();
    if (null != folder) {
        File[] fileArr = folder.listFiles();
        if (null != fileArr) {
            for (File fileEntry : fileArr) {
                if (fileEntry.getName().equals("topology.ns")) {
                    topology = fileEntry;
                    break;
                }
            }
        }
    }

    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(topology), "UTF-8"))) {
        String line = br.readLine();

        while (line != null) {
            // Looking for client
            // set clientPoolD [$ns node]
            if (line.contains("set clientPool")) {
//                String stringClient = line.split(" ")[1].replaceAll("clientPool", "");
//                clientSet.add(new StringRegionIdentifier(stringClient));
            }
            // Looking for all regions, nodes and edges
            // set linkBA [$ns duplex-link $nodeB $nodeA 100000.0kb 0.0ms
            // DropTail]
            else if (line.contains("set link") && !line.contains("linkClient") && line.contains("100000")) {
                line = line.substring(8, 10);
                String regionOne = line.substring(0, 1);
                String regionTwo = line.substring(1, 2);

                graph.addVertex(regionOne);
                graph.addVertex(regionTwo);
                graph.addEdge(line, regionOne, regionTwo);
            }
            // tb-set-hardware $nodeA large
            else if (line.contains("large")) {
//                rootRegion = line.split(" ")[1].replaceAll("\\$node", "");
            }
            line = br.readLine();
        }
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
        
    return new DijkstraShortestPath<>(graph);
  }
}
