package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class ReadRequest {

  public static final int FIRST_INSTANCE = 0;
  public static final int LAST_INSTANCE = 0;

  public static final int NUMBER_OF_INSTANCES = 10;
    
//  public static final int[] AGENTS = {5, 10, 15, 20};
  public static final int[] AGENTS = {10};
  
  public static final String ALGORITHM = "RC-DIFF";
//  public static final String ALGORITHM = "RDIFF";
  public static final String GRAPH = "scale-free-tree";
//  public static final String GRAPH = "random-network";
  
  public static final SortedMap<String, Request> regionResult = new TreeMap<>();
  
  public static final Map<Integer, SortedMap<String, Request>> regionResultOvertime = new HashMap<>();
  
  public static void main(String[] args) {
    int startTime = 30000;
    
    // Number of agents -> hop -> service -> successful/failed requests
    SortedMap<Integer, SortedMap<Integer, SortedMap<String, Request>>> result = new TreeMap<>();
    
    // Create a file from the directory
    // If the folder match "clientPoolX"
    //  Go to this folder
    //  Read the file client_requests_sent-clientPoolA.json
    //  Traverse this file:
    //    Read:
    //     "ncpContacted" : {
    //        "name" : "nodeA"
    //        "numClients" : 20,
    //        "artifact" : "test-service1",
    //        "serverResult" : "SUCCESS" || "serverResult" : "FAIL" || "serverResult" : "SLOW",
    for (int agent : AGENTS) {
      System.out.println("Agent: " + agent);      
      for (int instance = FIRST_INSTANCE; instance <= LAST_INSTANCE; instance++) {
        regionResult.clear();
        //         CDIFF/random-network/d5/0/output
        String directory = ALGORITHM + "/scenario/" + GRAPH + "/d" + agent + "/" + instance;
//        System.out.println(directory);
                    
        DijkstraShortestPath<String, String> pathFinder = getPathFinder(Paths.get(directory));
        
        String outputDirectory = directory + "/output";
        
        Path outputPath = Paths.get(outputDirectory);
        
        int totalSuccessfulRequests = 0;
        
        for (final File file : outputPath.toFile().listFiles()) {
          if (file.getName().contains("clientPool")) {
            String client = file.getName().replace("clientPool", "");
            String line;
            try {
              BufferedReader br = new BufferedReader(new FileReader(file.toString() + "/client_requests_sent-clientPool" + client + ".json"));
              String region = new String();
              int numRequest = 0;
              String service = new String();
              line = br.readLine();
              while (line != null && !line.isEmpty()) {
                if (line.contains("ncpContacted")) {
                  line = br.readLine(); // read the next line
                  line = line.replace(" ", "");
                  line = line.replace("\"", "");
                  region = line.replace("name:node", "");
                }
                else if (line.contains("numClients")) {
                  line = line.replace(" ", "");
                  line = line.replace("\"numClients\":", "");
//                  numRequest = Integer.parseInt(line.replace(",", ""));
                  numRequest = 1;
                }
                else if (line.contains("artifact")) {
                  line = line.replace(" ", "");
                  line = line.replace("\"artifact\":\"test-service", "");
                  service = line.replace(",", "");
                }
                else if (line.contains("serverResult")) {
                  if (line.contains("SUCCESS") || line.contains("SLOW")) {
                    incrementSuccess(result, agent, pathFinder.getDistance(region, client).intValue(), service, numRequest);
                    totalSuccessfulRequests += numRequest;
//                    System.out.println("Client=" + client + ", Region=" + region + ", numRequest=" + numRequest);
//                    System.out.println(result);
                    incrementRegionSuccess(regionResult, region);
                  }
                  else if (line.contains("FAIL")) {
//                    incrementFail(result, agent, pathFinder.getDistance(region, client).intValue(), service, numRequest);
                    incrementRegionFailure(regionResult, region);
                  }
                }
                
                line = br.readLine();
              }
              br.close();
            } catch (IOException e) {
              System.out.println(e);
            }
          }
        }
        int totalSucess = regionResult.entrySet().stream().mapToInt(e -> e.getValue().getSuccess()).sum();
        int totalFailure = regionResult.entrySet().stream().mapToInt(e -> e.getValue().getFail()).sum();
        
        System.out.println("Instance=" + instance);
        System.out.println("Success=" + totalSucess + ", Failure=" + totalFailure);
        System.out.println("RegionResult=" + regionResult);
      }
    }
    
    for (Entry<Integer, SortedMap<Integer, SortedMap<String, Request>>> entry : result.entrySet()) {
      System.out.println("Agent = " + entry.getKey());
      
      int total = 0;
      
      for (Entry<Integer, SortedMap<String, Request>> innerEntry : entry.getValue().entrySet()) {
        System.out.println("  Hops = " + innerEntry.getKey());
        int count = innerEntry.getValue().values().stream().mapToInt(Request::getSuccess).sum() / (LAST_INSTANCE - FIRST_INSTANCE + 1);
//        int count = innerEntry.getValue().values().stream().mapToInt(Request::getSuccess).sum();
        total += count;
        System.out.println("  " + count);
      }
      System.out.println("Total=" + total);
      
    }
  }

  public static void incrementSuccess(SortedMap<Integer, SortedMap<Integer, SortedMap<String, Request>>> result, int agent, int hop, String service, int numRequest) {
    SortedMap<Integer, SortedMap<String, Request>> hopMap = result.getOrDefault(agent, new TreeMap<>());
    SortedMap<String, Request> serviceMap = hopMap.getOrDefault(hop, new TreeMap<>());
    Request request = serviceMap.getOrDefault(service, new Request());
    
    request.incrementSuccess(numRequest);
    
    serviceMap.put(service, request);
    hopMap.put(hop, serviceMap);
    result.put(agent, hopMap);
  }
  
  public static void incrementFail(SortedMap<Integer, SortedMap<Integer, SortedMap<String, Request>>> result, int agent, int hop, String service, int numRequest) {
    SortedMap<Integer, SortedMap<String, Request>> hopMap = result.getOrDefault(agent, new TreeMap<>());
    SortedMap<String, Request> serviceMap = hopMap.getOrDefault(hop, new TreeMap<>());
    Request request = serviceMap.getOrDefault(service, new Request());
    
    request.incrementFail(numRequest);
    
    serviceMap.put(service, request);
    hopMap.put(hop, serviceMap);
    result.put(agent, hopMap);
  }
  
  public static void incrementRegionSuccess(SortedMap<String, Request> map, String region) {
    Request request = map.getOrDefault(region, new Request());
    request.incrementSuccess(1);
    map.put(region, request);
  }
  
  public static void incrementRegionFailure(SortedMap<String, Request> map, String region) {
    Request request = map.getOrDefault(region, new Request());
    request.incrementFail(1);
    map.put(region, request);
  }
  
  public static DijkstraShortestPath<String, String> getPathFinder(Path scenarioPath) {
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
    
//    System.out.println("Graph: " + graph);
    
    return new DijkstraShortestPath<>(graph);
  }
}
