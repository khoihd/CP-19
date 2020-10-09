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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class ReadRequest {

  public static final int NUMBER_OF_INSTANCES = 20;
    
  public static final int[] AGENTS = {15};
  
  public static final String ALGORITHM = "RDIFF";
  
  public static final String GRAPH = "scale-free-tree";
//  public static final String GRAPH = "random-network";
  
  public static void main(String[] args) {
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
      
      for (int instance = 0; instance < NUMBER_OF_INSTANCES; instance++) {
        //         CDIFF/random-network/d5/0/output
        String directory = ALGORITHM + "/scenario/" + GRAPH + "/d" + agent + "/" + instance;
        System.out.println(directory);
                    
        DijkstraShortestPath<String, String> pathFinder = getPathFinder(Paths.get(directory));
        
        String outputDirectory = directory + "/output";
        
        Path outputPath = Paths.get(outputDirectory);      
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
//                    System.out.println("Client=" + client + ", Region=" + region + ", numRequest=" + numRequest);
//                    System.out.println(result);
                  }
                  else if (line.contains("FAIL")) {
//                    incrementFail(result, agent, pathFinder.getDistance(region, client).intValue(), service, numRequest);
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
      }
    }
    
    for (Entry<Integer, SortedMap<Integer, SortedMap<String, Request>>> entry : result.entrySet()) {
      System.out.println("Agent = " + entry.getKey());
      
      for (Entry<Integer, SortedMap<String, Request>> innerEntry : entry.getValue().entrySet()) {
        System.out.println("  Hops = " + innerEntry.getKey());
        System.out.println("  " + innerEntry.getValue().values().stream().mapToInt(Request::getSuccess).sum() / NUMBER_OF_INSTANCES);
      }
      
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
