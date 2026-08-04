package it.polito.verefoo.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.polito.verefoo.VerefooSerializer;
import it.polito.verefoo.extra.Package1LoggingClass;
import it.polito.verefoo.extra.TestCaseGeneratorTexas2000;
import it.polito.verefoo.jaxb.FunctionalTypes;
import it.polito.verefoo.jaxb.Graph;
import it.polito.verefoo.jaxb.NFV;
import it.polito.verefoo.jaxb.Node;
import it.polito.verefoo.jaxb.PName;

/**
 * 
 * This class runs some tests to collect some data about the performance of the tool 
 * for the Texas2000 test case with varying numberUCC and numberSS parameters
 *
 */
public class TestPerformanceScalabilityTexas2000 {

    /* Variables to set if you want to automatically create the NFV */
    private static int runs;
    static String prefix = new String("Texas");
    static int currentSeed = 18471289;
    static int totalRuns = 1;
    static int seed;
    static Random rand;

	// LEIDEN SPECIFIC!!
	private static final float[] randomness = {0.1f, 0.2f};
	private static final String[] qualityFunction = {"Modularity", "CPM"};
	private static final String[] leidenSeed = {"999"};
	private static final int[] iterations = {5, 10, 20};
	private static final int[] minNodes = {1, 2, 4, 6};
	private static final String[] normalization = {"A", "N", "M"};
	private static final float [] resolutionParameter = {0.001f, 0.01f, 0.03f, 0.1f, 0.3f, 1.0f, 3f, 10f};
	private static String algo = "AP";
	private static BufferedWriter leidenFile;
	
	
    private static long totTime = 0;
    private static long maxTotTime = 0, minTotTime = 0;
    private static int nSAT = 0, i = 0, err = 0;
    static NFV root;
    static String pathfile;
    private static ch.qos.logback.classic.Logger logger;
    private static boolean withPorts = false; // Set to true if you want to include ports in the policies
    private static boolean withIsolation = true; // VARIABILE GLOBALE PER ISOLATION - MODIFICA QUI
    private static int isolationBound = 30; // -1 = unlimited, altrimenti numero massimo di isolation properties
    private static int numberUCC;
    private static int numberSS;

    // Property counters
    private static int isolationPropertiesCount = 0;
    private static int reachabilityPropertiesCount = 0;
    private static int totalPropertiesCount = 0;

    // Per-run accumulators (summed across SAT runs within the run)
    private static long sumNodes = 0;
    private static long sumPolicies = 0;
    private static long sumFW = 0;
    private static long sumRules = 0;

    public static void main(String[] args) {
        // Validate parameters consistency before starting tests
        if (!withIsolation && isolationBound != -1) {
            System.err.println("===============================================");
            System.err.println("FATAL ERROR: Parameter inconsistency detected!");
            System.err.println("withIsolation = " + withIsolation);
            System.err.println("isolationBound = " + isolationBound);
            System.err.println("Cannot specify isolationBound when withIsolation is false");
            System.err.println("Please fix parameters before running tests.");
            System.err.println("===============================================");
            System.exit(1);
        }

		System.out.println("===== TEST PERFORMANCE SCALABILITY TEXAS2000 ======");
        System.out.println("Parameters validation: OK");
        System.out.println("withIsolation = " + withIsolation + ", isolationBound = " + isolationBound);

        // Initialize JSON file at the beginning of the test
        initializeJsonFile();
        
        //LEIDEN LOGS!
        /*
        try {
			//leidenFile = Files.newBufferedWriter(Paths.get("leiden_results_AP_texas.csv"));
			//leidenFile.write(RunResultLeiden.csvHeader());
			//leidenFile.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}*/


        // Test cases: each array contains {numberUCC, numberSS} pairs
        int[][] testCases = {
        	{1, 1},
        	{1, 2},
        	{2, 1},
        	{2, 2},
        	{2, 3},
            {2, 4},
            {3, 1},
            {3, 2},
            {3, 3},
            {3, 4},
            {3, 5},
            {3, 6},
            {3, 7},
            {3, 8},
            {3, 9},
            {4, 1},
            {4, 4},
            {4, 8},
//            {10, 30},
//            {30, 90},
//            {75, 225}
        };

        for (int[] testCase : testCases) {

            // Collect execution times for all runs
            List<Long> runTimes = new ArrayList<>();
            int totalSATRuns = 0;
            int totalErrors = 0;
            
            sumNodes = 0;
            sumPolicies = 0;
            sumFW = 0;
            sumRules = 0;

            // Collect per-run averages for topology stats (like time)
            List<Long> runAvgNodes = new ArrayList<>();
            List<Long> runAvgPolicies = new ArrayList<>();
            List<Long> runAvgFW = new ArrayList<>();
            List<Long> runAvgRules = new ArrayList<>();

            for (int runNumber = 1; runNumber <= totalRuns; runNumber++) {

                boolean validRun = false;

                while (!validRun) {
                    // Complete reset for each individual run
                    seed = currentSeed;
                    numberUCC = testCase[0];
                    numberSS = testCase[1];
                    runs = 1;

                    totTime = 0;
                    maxTotTime = 0;
                    minTotTime = Integer.MAX_VALUE;
                    nSAT = 0;
                    i = 0;
                    err = 0;
                    root = null;
                    rand = null;
                    logger = null;

                    

                    // Reset property counters
                    isolationPropertiesCount = 0;
                    reachabilityPropertiesCount = 0;
                    totalPropertiesCount = 0;

                    int nodes_number = 3 + 17 * numberUCC + 14 * numberSS;
                    int firewalls_number = 1 + 3 * numberUCC + 2 * numberSS;
                    System.out.println("UCC=" + numberUCC + ", SS=" + numberSS + ", Nodes=" + nodes_number +
                            ", Firewalls=" + firewalls_number + ", WithIsolation=" + withIsolation + ", WithPorts=" + withPorts + " - Run " + runNumber);

                    try {
                        testScalabilityPerformance();

                        // Collect execution time for each run
                        if (nSAT > 0) {
                            long avgTimeRun = totTime / nSAT;
                            runTimes.add(avgTimeRun);

                            // Per-run averages for topology metrics
                            long avgNodesRun = sumNodes / nSAT;
                            long avgPoliciesRun = sumPolicies / nSAT;
                            long avgFWRun = sumFW / nSAT;
                            long avgRulesRun = sumRules / nSAT;

                            runAvgNodes.add(avgNodesRun);
                            runAvgPolicies.add(avgPoliciesRun);
                            runAvgFW.add(avgFWRun);
                            runAvgRules.add(avgRulesRun);

                            totalSATRuns += nSAT;
                            totalErrors += err;
                            validRun = true;

                        } else {
                            System.out.println("Run failed (UNSAT or Timeout). Retrying with new seed...");
                            Random r = new Random(currentSeed);
                            currentSeed = r.nextInt();
                        }

                    } catch (Exception e) {
                        System.err.println("Run " + runNumber + " failed: " + e.getMessage());
                        System.out.println("Retrying with new seed...");
                        Random r = new Random(currentSeed);
                        currentSeed = r.nextInt();
                    }
                }

                // Update JSON file after each successful run
                if (!runTimes.isEmpty()) {

                    // Progressive statistics for time
                    long avgTime = runTimes.stream().mapToLong(Long::longValue).sum() / runTimes.size();
                    long maxTime = runTimes.stream().mapToLong(Long::longValue).max().orElse(0);
                    long minTime = runTimes.stream().mapToLong(Long::longValue).min().orElse(0);

                    // Progressive statistics for topology metrics
                    long avgNodes = runAvgNodes.stream().mapToLong(Long::longValue).sum() / runAvgNodes.size();
                    long maxNodes = runAvgNodes.stream().mapToLong(Long::longValue).max().orElse(0);
                    long minNodes = runAvgNodes.stream().mapToLong(Long::longValue).min().orElse(0);

                    long avgPolicies = runAvgPolicies.stream().mapToLong(Long::longValue).sum() / runAvgPolicies.size();
                    long maxPolicies = runAvgPolicies.stream().mapToLong(Long::longValue).max().orElse(0);
                    long minPolicies = runAvgPolicies.stream().mapToLong(Long::longValue).min().orElse(0);

                    long avgFW = runAvgFW.stream().mapToLong(Long::longValue).sum() / runAvgFW.size();
                    long maxFW = runAvgFW.stream().mapToLong(Long::longValue).max().orElse(0);
                    long minFW = runAvgFW.stream().mapToLong(Long::longValue).min().orElse(0);

                    long avgRules = runAvgRules.stream().mapToLong(Long::longValue).sum() / runAvgRules.size();
                    long maxRules = runAvgRules.stream().mapToLong(Long::longValue).max().orElse(0);
                    long minRules = runAvgRules.stream().mapToLong(Long::longValue).min().orElse(0);

                    System.out.println("=== PARTIAL RESULTS AFTER RUN " + runNumber + " (UCC=" + numberUCC + ", SS=" + numberSS + ") ===");
                    System.out.println("AVG time (" + runTimes.size() + " run" + (runTimes.size() > 1 ? "s" : "") + "): " + avgTime + "ms");
                    System.out.println("MAX time: " + maxTime + "ms");
                    System.out.println("MIN time: " + minTime + "ms");
                    System.out.println("SAT runs so far: " + totalSATRuns + "/" + runNumber);
                    System.out.println("Errors so far: " + totalErrors);

                    System.out.println("AVG nodes: " + avgNodes + " (min=" + minNodes + ", max=" + maxNodes + ")");
                    System.out.println("AVG policies: " + avgPolicies + " (min=" + minPolicies + ", max=" + maxPolicies + ")");
                    System.out.println("AVG firewalls: " + avgFW + " (min=" + minFW + ", max=" + maxFW + ")");
                    System.out.println("AVG rules: " + avgRules + " (min=" + minRules + ", max=" + maxRules + ")");

                    // Create result object and update JSON immediately
                    Map<String, Object> result = new HashMap<>();
                    result.put("ucc", numberUCC);
                    result.put("ss", numberSS);
                    result.put("nodes", 3 + 17 * numberUCC + 14 * numberSS);
                    result.put("firewalls", 1 + 3 * numberUCC + 2 * numberSS);

                    result.put("avgTime", avgTime);
                    result.put("maxTime", maxTime);
                    result.put("minTime", minTime);

                    // NEW: topology stats (progressive like time)
                    result.put("avgNodes", avgNodes);
                    result.put("minNodes", minNodes);
                    result.put("maxNodes", maxNodes);

                    result.put("avgPolicies", avgPolicies);
                    result.put("minPolicies", minPolicies);
                    result.put("maxPolicies", maxPolicies);

                    result.put("avgFW", avgFW);
                    result.put("minFW", minFW);
                    result.put("maxFW", maxFW);

                    result.put("avgRules", avgRules);
                    result.put("minRules", minRules);
                    result.put("maxRules", maxRules);

                    result.put("satRuns", totalSATRuns);
                    result.put("errors", totalErrors);

                    result.put("isolationNumber", isolationPropertiesCount);
                    result.put("reachabilityNumber", reachabilityPropertiesCount);
                    result.put("totProperty", totalPropertiesCount);

                    result.put("withPorts", withPorts);
                    result.put("withIsolation", withIsolation);
                    result.put("isolationBound", isolationBound);
                    result.put("currentRun", runNumber);
                    result.put("totalRuns", totalRuns);

                    // Update JSON file immediately after each run
                    updateJsonWithNewResult(result);

                } else if (runNumber == totalRuns) {
                    // If all runs failed, save failure information
                    System.out.println("=== NO SUCCESSFUL RUNS FOR UCC=" + numberUCC + ", SS=" + numberSS + " AFTER " + runNumber + " ATTEMPTS ===");

                    Map<String, Object> result = new HashMap<>();
                    result.put("ucc", numberUCC);
                    result.put("ss", numberSS);
                    result.put("nodes", 3 + 17 * numberUCC + 14 * numberSS);
                    result.put("firewalls", 1 + 3 * numberUCC + 2 * numberSS);

                    result.put("avgTime", -1);
                    result.put("maxTime", -1);
                    result.put("minTime", -1);

                    // NEW: topology stats failure markers
                    result.put("avgNodes", -1);
                    result.put("minNodes", -1);
                    result.put("maxNodes", -1);

                    result.put("avgPolicies", -1);
                    result.put("minPolicies", -1);
                    result.put("maxPolicies", -1);

                    result.put("avgFW", -1);
                    result.put("minFW", -1);
                    result.put("maxFW", -1);

                    result.put("avgRules", -1);
                    result.put("minRules", -1);
                    result.put("maxRules", -1);

                    result.put("satRuns", 0);
                    result.put("errors", totalErrors);

                    result.put("isolationNumber", 0);
                    result.put("reachabilityNumber", 0);
                    result.put("totProperty", 0);

                    result.put("withPorts", withPorts);
                    result.put("withIsolation", withIsolation);
                    result.put("isolationBound", isolationBound);
                    result.put("currentRun", runNumber);
                    result.put("totalRuns", totalRuns);

                    updateJsonWithNewResult(result);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("==========================================\n");
        }

        System.out.println("All test cases completed. Final JSON file contains all results up to this point.");
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

//    private static NFV testCoarse(NFV root) throws Exception {
//        long beginAll = System.currentTimeMillis();
//
//        // Count properties before executing the test
//        countProperties(root);
//
////        VerefooSerializer test = new VerefooSerializer(root);
//        BufferedWriter file = Files.newBufferedWriter(Path.of("leiden_results.csv"));
//		file.write(RunResultLeiden.csvHeader());
//		file.newLine();
//
//		final float randomness = 0.1f;
//		final String qualityFunction = "Modularity";
//		final String leidenSeed = "999";
//		final int iterations = 20;
//		final int minNodes = 1;
//		final String normalization = "N";
//		final float resolutionParameter = 0.01f;
//		final String algo = "AP";
//		
//        VerefooSerializer test = new  VerefooSerializer(root,algo,resolutionParameter,minNodes,normalization,iterations,leidenSeed,qualityFunction,randomness, file);
//
//        long endAll = System.currentTimeMillis();
//        long runTime = endAll - beginAll;
//
//        if (test.isSat()) {
//            nSAT++;
//            maxTotTime = Math.max(maxTotTime, runTime);
//            minTotTime = Math.min(minTotTime, runTime);
//            System.out.println("SAT - " + runTime + "ms");
//            logger.debug("time: " + runTime + "ms;");
//            totTime += runTime;
//
//        } else {
//            System.out.println("UNSAT (potrebbe essere timeout del Checker)");
//            logger.debug("UNSAT");
//
//            // NOTE: nel tuo codice originale incrementi nSAT anche qui: mantengo il comportamento.
//            nSAT++;
//            maxTotTime = Math.max(maxTotTime, runTime);
//            minTotTime = Math.min(minTotTime, runTime);
//            logger.debug("time: " + runTime + "ms;");
//            totTime += runTime;
//        }
//
//        return test.getResult();
//    }
    
//    private static List<NFV> testCoarse(NFV root) throws Exception {
//    	List<NFV> res = new ArrayList<>();
//
//        // Count properties before executing the test
//        countProperties(root);
//
//		for(float resolutionP: resolutionParameter) {
//			for(int minN: minNodes) {
//				for(String norm: normalization) {
//					for(int iter: iterations) {
//						for(String seed: leidenSeed) {
//							for(String qFun: qualityFunction) {
//								for(float rand: randomness) {
//									/*
//									 * Root is modified during each run, specifically NSR of kind REACHABILITY might be promoted to COMPLETE_REACHABILITY.
//									 * Need to manually reset this here.
//									 */
//									if(root.getPropertyDefinition().getProperty().stream().anyMatch(p -> p.getName() == PName.COMPLETE_REACHABILITY_PROPERTY))
//										root.getPropertyDefinition().getProperty().stream().forEach(p -> {
//											if(p.getName()==PName.COMPLETE_REACHABILITY_PROPERTY)
//												p.setName(PName.REACHABILITY_PROPERTY);
//										});
//									long beginAll=System.currentTimeMillis();
//									VerefooSerializer test = new  VerefooSerializer(root,algo,resolutionP, minN, norm, iter, seed, qFun, rand, leidenFile);
//									long endAll=System.currentTimeMillis();
//							        long runTime = endAll - beginAll;
//									if(test.isSat()){
//										nSAT++;
//							            maxTotTime = Math.max(maxTotTime, runTime);
//							            minTotTime = Math.min(minTotTime, runTime);
//							            System.out.println("SAT - " + runTime + "ms");
//							            logger.debug("time: " + runTime + "ms;");
//							            totTime += runTime;
//									 }
//								 	else{
//							            System.out.println("UNSAT (potrebbe essere timeout del Checker)");
//							            logger.debug("UNSAT");
//							            nSAT++;
//								 	}
//									res.add(test.getResult());
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//        
//		return res;
//    }

    /**
     * Counts isolation and reachability properties in the NFV
     */
    private static void countProperties(NFV nfv) {
        isolationPropertiesCount = 0;
        reachabilityPropertiesCount = 0;

        if (nfv.getPropertyDefinition() != null && nfv.getPropertyDefinition().getProperty() != null) {
            for (it.polito.verefoo.jaxb.Property property : nfv.getPropertyDefinition().getProperty()) {
                if (property.getName() != null) {
                    String propertyName = property.getName().value();
                    if (propertyName != null) {
                        if (propertyName.toLowerCase().contains("isolation")) {
                            isolationPropertiesCount++;
                        } else if (propertyName.toLowerCase().contains("reachability")) {
                            reachabilityPropertiesCount++;
                        }
                    }
                }
            }
        }

        totalPropertiesCount = isolationPropertiesCount + reachabilityPropertiesCount;

        System.out.println("Properties found - Isolation: " + isolationPropertiesCount +
                ", Reachability: " + reachabilityPropertiesCount +
                ", Total: " + totalPropertiesCount);
    }
    
    private static NFV testCoarse(NFV root) throws Exception {
        long beginAll = System.currentTimeMillis();

        // Count properties before executing the test
        countProperties(root);

        VerefooSerializer test = new VerefooSerializer(root, "AP", true);

        long endAll = System.currentTimeMillis();
        long runTime = endAll - beginAll;

        if (test.isSat()) {
            nSAT++;
            maxTotTime = Math.max(maxTotTime, runTime);
            minTotTime = Math.min(minTotTime, runTime);
            System.out.println("SAT - " + runTime + "ms");
            logger.debug("time: " + runTime + "ms;");
            totTime += runTime;

        } else {
            System.out.println("UNSAT (potrebbe essere timeout del Checker)");
            logger.debug("UNSAT");

            // NOTE: nel tuo codice originale incrementi nSAT anche qui: mantengo il comportamento.
            nSAT++;
            maxTotTime = Math.max(maxTotTime, runTime);
            minTotTime = Math.min(minTotTime, runTime);
            logger.debug("time: " + runTime + "ms;");
            totTime += runTime;
        }

        return test.getResult();
    }


    @Test
    public static void testScalabilityPerformance() {

        // Complete initialization for each call
        rand = new Random(seed);
        pathfile = "VerefooMemoryTexas.log";
        logger = Package1LoggingClass.createLoggerFor(pathfile, "log/" + pathfile);

        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory();
        long maxMem = rt.maxMemory();
        long freeMem = rt.freeMemory();
        double megs = 1048576.0;

        System.out.println("Total Memory: " + totalMem + " (" + (totalMem / megs) + " MiB)");
        System.out.println("Max Memory:   " + maxMem + " (" + (maxMem / megs) + " MiB)");
        System.out.println("Free Memory:  " + freeMem + " (" + (freeMem / megs) + " MiB)");

        int[] seeds = new int[runs];
        for (int m = 0; m < runs; m++) {
            seeds[m] = Math.abs(rand.nextInt());
        }

        int k = 0;
        try {
            List<TestCaseGeneratorTexas2000> nfv = new ArrayList<>();

            // Attempt to create generator - if there are parameter errors, stop execution
            try {
                nfv.add(new TestCaseGeneratorTexas2000(seed, numberUCC, numberSS, withPorts, withIsolation, isolationBound));
            } catch (IllegalArgumentException e) {
                System.err.println("===============================================");
                System.err.println("FATAL ERROR during generator creation:");
                System.err.println(e.getMessage());
                System.err.println("Terminating execution - no JSON will be generated.");
                System.err.println("===============================================");
                System.exit(1);
            }

            for (TestCaseGeneratorTexas2000 f : nfv) {
                logger.info("===========FILE " + f.getName() + " UCC=" + numberUCC + " SS=" + numberSS + "===========");

                JAXBContext jc = JAXBContext.newInstance("it.polito.verefoo.jaxb");
                Unmarshaller u = jc.createUnmarshaller();
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = sf.newSchema(new File("./xsd/nfvSchema.xsd"));
                u.setSchema(schema);

                Marshaller m = jc.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "./xsd/nfvSchema.xsd");

                // Execute only 1 run (runs = 1)
                for (k = 0; k < runs; k++) {
                    try {

                        TestCaseGeneratorTexas2000 currentGenerator;
                        try {
                            currentGenerator = new TestCaseGeneratorTexas2000(seeds[k], numberUCC, numberSS, withPorts, withIsolation, isolationBound);
                        } catch (IllegalArgumentException e) {
                            System.err.println("===============================================");
                            System.err.println("FATAL ERROR during generator creation (seed: " + seeds[k] + "):");
                            System.err.println(e.getMessage());
                            System.err.println("Terminating execution - no JSON will be generated.");
                            System.err.println("===============================================");
                            System.exit(1);
                            return;
                        }

                        root = currentGenerator.generateNFV(numberUCC, numberSS, withPorts, withIsolation, isolationBound);

                        i++;

                        NFV resultNFV = testCoarse(root); // vanilla
//                        List<NFV> resultNFV = testCoarse(root); // leiden
                        
                        // Topology stats on RESULT NFV
//                        RunStats stats = analyzeNFV(resultNFV, m);
//                        sumNodes += stats.nodes;
//                        sumPolicies += stats.policies;
//                        sumFW += stats.firewalls;
//                        sumRules += stats.rules;

                        // Immediate cleanup
                        root = null;
                        currentGenerator = null;

                    } catch (Exception e) {
                        System.err.println("Error in single run:");
                        e.printStackTrace();
                        err++;
                    }
                }

                logger.info("Simulations -> " + k + " / Errors -> " + err);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the JSON file by clearing it at the beginning of the test
     */
    private static void initializeJsonFile() {
        try (FileWriter writer = new FileWriter("dataToPlot.json")) {
            writer.write("{\n");
            writer.write("  \"testResults\": [],\n");
            writer.write("  \"timestamp\": \"" + java.time.LocalDateTime.now().toString() + "\",\n");
            writer.write("  \"totalTestCases\": 0\n");
            writer.write("}\n");

            System.out.println("JSON file initialized - dataToPlot.json cleared and ready for new results");

        } catch (IOException e) {
            System.err.println("Error initializing JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Writes results to JSON file
     */
    private static void writeResultsToJson(List<Map<String, Object>> results) {
        try (FileWriter writer = new FileWriter("dataToPlot.json")) {
            writer.write("{\n");
            writer.write("  \"testResults\": [\n");

            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> result = results.get(i);
                writer.write("    {\n");
                writer.write("      \"ucc\": " + result.get("ucc") + ",\n");
                writer.write("      \"ss\": " + result.get("ss") + ",\n");
                writer.write("      \"nodes\": " + result.get("nodes") + ",\n");
                writer.write("      \"firewalls\": " + result.get("firewalls") + ",\n");

                writer.write("      \"avgTime\": " + result.get("avgTime") + ",\n");
                writer.write("      \"maxTime\": " + result.get("maxTime") + ",\n");
                writer.write("      \"minTime\": " + result.get("minTime") + ",\n");

                // NEW: topology stats
                writer.write("      \"avgNodes\": " + result.get("avgNodes") + ",\n");
                writer.write("      \"minNodes\": " + result.get("minNodes") + ",\n");
                writer.write("      \"maxNodes\": " + result.get("maxNodes") + ",\n");

                writer.write("      \"avgPolicies\": " + result.get("avgPolicies") + ",\n");
                writer.write("      \"minPolicies\": " + result.get("minPolicies") + ",\n");
                writer.write("      \"maxPolicies\": " + result.get("maxPolicies") + ",\n");

                writer.write("      \"avgFW\": " + result.get("avgFW") + ",\n");
                writer.write("      \"minFW\": " + result.get("minFW") + ",\n");
                writer.write("      \"maxFW\": " + result.get("maxFW") + ",\n");

                writer.write("      \"avgRules\": " + result.get("avgRules") + ",\n");
                writer.write("      \"minRules\": " + result.get("minRules") + ",\n");
                writer.write("      \"maxRules\": " + result.get("maxRules") + ",\n");

                writer.write("      \"satRuns\": " + result.get("satRuns") + ",\n");
                writer.write("      \"errors\": " + result.get("errors") + ",\n");
                writer.write("      \"isolationNumber\": " + result.get("isolationNumber") + ",\n");
                writer.write("      \"reachabilityNumber\": " + result.get("reachabilityNumber") + ",\n");
                writer.write("      \"totProperty\": " + result.get("totProperty") + ",\n");
                writer.write("      \"withPorts\": " + result.get("withPorts") + ",\n");
                writer.write("      \"withIsolation\": " + result.get("withIsolation") + ",\n");
                writer.write("      \"isolationBound\": " + result.get("isolationBound") + ",\n");
                writer.write("      \"currentRun\": " + result.get("currentRun") + ",\n");
                writer.write("      \"totalRuns\": " + result.get("totalRuns") + "\n");
                writer.write("    }");

                if (i < results.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  ],\n");
            writer.write("  \"timestamp\": \"" + java.time.LocalDateTime.now().toString() + "\",\n");
            writer.write("  \"totalTestCases\": " + results.size() + "\n");
            writer.write("}\n");

            System.out.println("Results exported to dataToPlot.json");

        } catch (IOException e) {
            System.err.println("Error writing JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Incrementally updates the JSON file with a new result.
     * If an entry for the same (UCC, SS) pair already exists, it replaces it.
     */
    private static void updateJsonWithNewResult(Map<String, Object> newResult) {
        List<Map<String, Object>> existingResults = new ArrayList<>();

        // Read existing results if file exists
        File jsonFile = new File("dataToPlot.json");
        if (jsonFile.exists()) {
            existingResults = readExistingResults();
        }

        // Remove any existing entries for the same (UCC, SS) pair to avoid duplicates in JSON
        existingResults.removeIf(result ->
                result.get("ucc").equals(newResult.get("ucc")) &&
                result.get("ss").equals(newResult.get("ss"))
        );

        // Add the new result
        existingResults.add(newResult);

        // Rewrite the entire JSON file
        writeResultsToJson(existingResults);

        System.out.println("JSON updated incrementally with new result (UCC=" + newResult.get("ucc") +
                ", SS=" + newResult.get("ss") + ", Run=" + newResult.get("currentRun") + "/" + newResult.get("totalRuns") + ")");
    }

    /**
     * Reads existing results from the JSON file
     */
    private static List<Map<String, Object>> readExistingResults() {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("dataToPlot.json")));

            // Simple JSON parsing (without external libraries)
            int startIndex = content.indexOf("\"testResults\": [");
            if (startIndex == -1) return results;

            startIndex = content.indexOf("[", startIndex) + 1;
            int endIndex = content.indexOf("],", startIndex);
            if (endIndex == -1) endIndex = content.indexOf("]", startIndex);

            String resultsSection = content.substring(startIndex, endIndex).trim();

            // Split by JSON objects (simple parsing)
            String[] jsonObjects = resultsSection.split("\\},\\s*\\{");

            for (String jsonObj : jsonObjects) {
                if (jsonObj.trim().isEmpty()) continue;

                jsonObj = jsonObj.trim();
                if (!jsonObj.startsWith("{")) jsonObj = "{" + jsonObj;
                if (!jsonObj.endsWith("}")) jsonObj = jsonObj + "}";

                Map<String, Object> result = parseJsonObject(jsonObj);
                if (!result.isEmpty()) {
                    results.add(result);
                }
            }

        } catch (Exception e) {
            System.err.println("Error reading existing JSON file: " + e.getMessage());
        }

        return results;
    }

    /**
     * Simple parsing of a JSON object (without external libraries)
     */
    private static Map<String, Object> parseJsonObject(String jsonStr) {
        Map<String, Object> result = new HashMap<>();

        try {
            jsonStr = jsonStr.replaceAll("[{}]", "").trim();
            String[] fields = jsonStr.split(",");

            for (String field : fields) {
                String[] keyValue = field.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim();

                    Object convertedValue;
                    if (value.equals("true") || value.equals("false")) {
                        convertedValue = Boolean.parseBoolean(value);
                    } else if (value.matches("-?\\d+")) {
                        convertedValue = Integer.parseInt(value);
                    } else {
                        convertedValue = value.replaceAll("\"", "");
                    }

                    result.put(key, convertedValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON object: " + e.getMessage());
        }

        return result;
    }

    private static class RunStats {
        int nodes = 0;
        int policies = 0;
        int firewalls = 0;
        int rules = 0;
        long elapsedMs = 0;
    }

    private static RunStats analyzeNFV(NFV nfv, Marshaller marshaller) {
        int numberNodes = 0;
        int numPolicy = 0;
        int numFW = 0;
        int numRules = 0;

        RunStats s = new RunStats();

        if (nfv.getPropertyDefinition() != null && nfv.getPropertyDefinition().getProperty() != null) {
            numPolicy = nfv.getPropertyDefinition().getProperty().size();
        }

        if (nfv.getGraphs() != null) {
            for (Graph g : nfv.getGraphs().getGraph()) {
                List<Node> nodes = g.getNode();
                if (nodes != null) {
                    numberNodes += nodes.size();
                    for (Node n : nodes) {
                        if (n.getConfiguration() != null) {
                            if (n.getFunctionalType() == FunctionalTypes.FIREWALL) {
                                numFW++;
                                if (n.getConfiguration().getFirewall() != null
                                        && n.getConfiguration().getFirewall().getElements() != null
                                        && !n.getConfiguration().getFirewall().getElements().isEmpty()) {
                                    numRules += (int) n.getConfiguration().getFirewall().getElements().size();
                                } else {
                                    numRules += n.getConfiguration().getFirewall().getDefaultAction() == null ? 0 : 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        s.nodes = numberNodes;
        s.policies = numPolicy;
        s.firewalls = numFW;
        s.rules = numRules;
        return s;
    }
}
