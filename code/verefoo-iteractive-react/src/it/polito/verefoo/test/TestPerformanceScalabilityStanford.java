package it.polito.verefoo.test;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import it.polito.verefoo.VerefooSerializer;
import it.polito.verefoo.extra.TestCaseGeneratorStanford;
import it.polito.verefoo.jaxb.FunctionalTypes;
import it.polito.verefoo.jaxb.Graph;
import it.polito.verefoo.jaxb.NFV;
import it.polito.verefoo.jaxb.Node;
import it.polito.verefoo.jaxb.PName;

public class TestPerformanceScalabilityStanford {

	public static void main(String[] args)  {
        
		System.out.println("The algorithm used is : AP"); //always MF in this branch
		
		seed  = 999;
		runs = 2;
		testScalabilityPerformance();
		
	}
	
	private static int runs;
	static String prefix = "Stanford";
	static int seed;
	static Random rand;
	
	private static long totTime = 0;
	private static long totMaxSMTTime = 0, totClusteringTime = 0, totMergeTime = 0;
	private static long maxTotTime = 0,minTotTime = 0;
	private  static int nSAT = 0, nUNSAT = 0, err = 0;
	static NFV root;
	static String pathfile;
	private static final int[] numberPRs = {10, 20};//, 30};//, 40, 50, 60};//, 80, 100};//, 300, 600, 1200};
	private final static double[][] percentPairs = {
		{0.5, 0.0},   // {reachabilityPercent, completeReachabilityPercent}
//		{0.25, 0.45},
//		{0.75, 0.15}
	};
	private final static double[] portSpecifics = {0.05};//, 0.3, 0.5, 0.75};
	
	
	// LEIDEN SPECIFIC!!
	private static final float[] randomness = {0.1f, 0.2f};
	private static final String[] qualityFunction = {"Modularity", "CPM"};
	private static final String[] leidenSeed = {"999"};
	private static final int[] iterations = {5, 10, 20};
	private static final int[] minNodes = {1, 2, 4, 6};
	private static final String[] normalization = {"A", "N", "M"};
	private static final float [] resolutionParameter = {0.001f, 0.01f, 0.03f, 0.1f, 0.3f, 1.0f, 3f, 10f};
	
	
	
	// helper class to hold test configuration parameters
	private static class TestParams {
		final int numberPR;
		final double reach;
		final double complete;
		final double port;

		TestParams(int numberPR, double reach, double complete, double port) {
			this.numberPR = numberPR; this.reach = reach; this.complete = complete; this.port = port;
		}
	}

	private static class RunStats {
        int nodes = 0;
        int policies = 0;
        int firewalls = 0;
        int rules = 0;
        long elapsedMs = 0;
    }
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	// Version for "VEREFOO Parallel"
	private static List<NFV> testCoarse(NFV root, BufferedWriter file) throws Exception{
		List<NFV> res = new ArrayList<>();

		//String algo = "MF";
		String algo = "AP";
		float[] randomness = {0.1f};
		String[] qualityFunction = {"Modularity"};
		String[] leidenSeed = {"999"};
		int[] iterations = {5};
		int[] minNodes = {4};
		String[] normalization = {"N"};
		float [] resolutionParameter = { 0.1f};
		for(float resolutionP: resolutionParameter) {
			for(int minN: minNodes) {
				for(String norm: normalization) {
					for(int iter: iterations) {
						for(String seed: leidenSeed) {
							for(String qFun: qualityFunction) {
								for(float rand: randomness) {
									/*
									 * Root is modified during each run, specifically NSR of kind REACHABILITY might be promoted to COMPLETE_REACHABILITY.
									 * Need to manually reset this here.
									 */
									if(root.getPropertyDefinition().getProperty().stream().anyMatch(p -> p.getName() == PName.COMPLETE_REACHABILITY_PROPERTY))
										root.getPropertyDefinition().getProperty().stream().forEach(p -> {
											if(p.getName()==PName.COMPLETE_REACHABILITY_PROPERTY)
												p.setName(PName.REACHABILITY_PROPERTY);
										});
									long beginAll=System.currentTimeMillis();
									// -----
									//VerefooSerializer test = new  VerefooSerializer(root,algo,resolutionP, minN, norm, iter, seed, qFun, rand, file);
									//VerefooSerializer test = new VerefooSerializer(root, algo, true);
									VerefooSerializer test = new VerefooSerializer(root, algo);
									// -----
									long endAll=System.currentTimeMillis();
									if(test.isSat()){
										nSAT++;
										maxTotTime = maxTotTime<(endAll-beginAll)? (endAll-beginAll) : maxTotTime;
										minTotTime = minTotTime>(endAll-beginAll)? (endAll-beginAll) : minTotTime;
										totTime += (endAll-beginAll);
										// ------
										/*
										totMaxSMTTime += test.getTestTimeResults().getEndMaxSMTtime();
										totClusteringTime += test.getTestTimeResults().getEndClusterizationTime() - test.getTestTimeResults().getStartClusterizationTime();
										totMergeTime += test.getTestTimeResults().getStartMergeTime() - test.getTestTimeResults().getStartMergeTime();
									  */
									  totMaxSMTTime += 0;
									  // -----
                    totClusteringTime += 0;
                    totMergeTime += 0;
									 }
								 	else{
										nUNSAT++;
								 	}
									res.add(test.getResult());
								}
							}
						}
					}
				}
			}
		}
//		VerefooSerializer test = new  VerefooSerializer(root,algo,resolutionParameter, minNodes, normalization, iterations, seed, qualityFunction, randomness, file);
        return res;
	}
	
	// Version for "VEREFOO"
	
	private static NFV testCoarse(NFV root) throws Exception{
		long beginAll=System.currentTimeMillis();
		// -----
		//String algo = "MF";
		String algo = "AP";
		// -----
		//VerefooSerializer test = new  VerefooSerializer(root,algo);
		VerefooSerializer test = new  VerefooSerializer(root,algo, true);
		long endAll=System.currentTimeMillis();

		if(test.isSat()){
			nSAT++;
			maxTotTime = maxTotTime<(endAll-beginAll)? (endAll-beginAll) : maxTotTime;
			minTotTime = minTotTime>(endAll-beginAll)? (endAll-beginAll) : minTotTime;
			totTime += (endAll-beginAll);
			//totMaxSMTTime += test.getTestTimeResults().getEndMaxSMTtime() - test.getTestTimeResults().getBeginMaxSMTTime();
			totMaxSMTTime += 0;
		}else{
			nUNSAT++;
	 	}
        return test.getResult();
	}
	
	// Use this for testing with Leiden (receives and writes on file the data for the clustering tests)
	
	/*
	public static void testScalabilityPerformance(){
		rand= new Random(seed);
		Runtime rt = Runtime.getRuntime();
		long totalMem = rt.totalMemory();
		long maxMem = rt.maxMemory();
		long freeMem = rt.freeMemory();
		double megs = 1048576.0;
		long sumTime = 0;
        long sumNodes = 0;
        long sumPolicies = 0;
        long sumFW = 0;
        long sumRules = 0;
        long sumMaxSMTTime;
        long sumClusteringTime;
        long sumMergeTime;

        int totalRuns = runs; // num of experiments, to be used for averages

		List<TestParams> configs = new ArrayList<>();
		// generate the test configurations (combinations of parameters)
		for (int pr : numberPRs) {
			for (double[] pair : percentPairs) {
				for (double port : portSpecifics) {
					configs.add(new TestParams(pr, pair[0], pair[1], port));
				}
			}
		}
		
		
		System.out.println ("Total Memory: " + totalMem + " (" + (totalMem/megs) + " MiB)");
		System.out.println ("Max Memory:   " + maxMem + " (" + (maxMem/megs) + " MiB)");
		System.out.println ("Free Memory:  " + freeMem + " (" + (freeMem/megs) + " MiB)");
	

		int[] seeds = new int[runs];
		
		for(int m=0;m<runs;m++) { 
			seeds[m]=Math.abs(rand.nextInt()); 
		}
		// Create a log file to write the results of the tests
		BufferedWriter resFile = null;
		try {
			resFile = Files.newBufferedWriter(Paths.get("results_stanford.log"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}

		int k=0;
		try (BufferedWriter file = Files.newBufferedWriter(Paths.get("leiden_results_MF_stanford.csv"))) {
		    file.write(RunResultLeiden.csvHeader());
		    file.newLine();

		    for (TestParams cfg : configs) {
		        sumTime = 0;
		        sumNodes = 0;
		        sumPolicies = 0;
		        sumFW = 0;
		        sumRules = 0;
		        sumMaxSMTTime = 0;
		        sumClusteringTime = 0;
		        sumMergeTime = 0;

		        for (k = 0; k < runs; k++) {
		            TestCaseGeneratorStanford f = new TestCaseGeneratorStanford(
		                prefix + cfg.numberPR + "PR_r" + (cfg.reach) + "_c" + (cfg.complete) + "_p" + (cfg.port),
		                cfg.numberPR,
		                seed,
		                cfg.reach,
		                cfg.complete,
		                cfg.port
		            );

		            totTime = 0;
		            maxTotTime = 0;
		            minTotTime = Integer.MAX_VALUE;
		            totMaxSMTTime = 0;
		            totClusteringTime = 0;
		            totMergeTime = 0;
		            

		            JAXBContext jc = JAXBContext.newInstance("it.polito.verefoo.jaxb");
		            Unmarshaller u = jc.createUnmarshaller();
		            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		            Schema schema = sf.newSchema(new File("./xsd/nfvSchema.xsd"));
		            u.setSchema(schema);

		            Marshaller m = jc.createMarshaller();
		            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		            m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "./xsd/nfvSchema.xsd");

		            try {
		                m = jc.createMarshaller();
		                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		                m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "./xsd/nfvSchema.xsd");

		                root = f.getNfv();
		                long t0 = System.currentTimeMillis();
		                List<NFV> resultNFV = testCoarse(root, file);
		                long elapsed = System.currentTimeMillis() - t0;

		                JAXBContext context = JAXBContext.newInstance("it.polito.verefoo.jaxb");
		                Marshaller marshaller = context.createMarshaller();
		                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		                
		                for(NFV res: resultNFV) {
		                	RunStats stats = analyzeNFV(res, m);
		                	stats.elapsedMs = elapsed;

		                	sumTime += stats.elapsedMs;
		                	sumNodes += stats.nodes;
		                	sumPolicies += stats.policies;
		                	sumFW += stats.firewalls;
		                	sumRules += stats.rules;
		                	sumMaxSMTTime += totMaxSMTTime;
		                	sumClusteringTime += totClusteringTime;
		                	sumMergeTime += totMergeTime;
		                	
		                	// Print individual run details
		                	String runDetail = String.format("  Run %d: time=%dms nodes=%d policies=%d fw=%d rules=%d maxSMT=%dms clustering=%dms merge=%dms", 
		                		k, stats.elapsedMs, stats.nodes, stats.policies, stats.firewalls, stats.rules, totMaxSMTTime, totClusteringTime, totMergeTime);
		                	System.out.println(runDetail);
                	if (resFile != null) {
                		resFile.write(runDetail + "\n");
                		resFile.flush();
                	}
		                }

		            } catch (Exception e) {
		                e.printStackTrace();
		                err++;
		            }
		        }

		        double avgTime = totalRuns > 0 ? ((double) sumTime) / totalRuns : 0;
		        double avgNodes = totalRuns > 0 ? ((double) sumNodes) / totalRuns : 0;
		        double avgPolicies = totalRuns > 0 ? ((double) sumPolicies) / totalRuns : 0;
		        double avgFW = totalRuns > 0 ? ((double) sumFW) / totalRuns : 0;
		        double avgRules = totalRuns > 0 ? ((double) sumRules) / totalRuns : 0;

		        
		        double avgClustering = totalRuns > 0? ((double) sumClusteringTime) / totalRuns : 0;
		        double avgMaxSMT = totalRuns > 0? ((double) sumMaxSMTTime) / totalRuns : 0;
		        double avgMerge = totalRuns > 0? ((double) sumMergeTime) / totalRuns : 0;
		        // Write results to file for Leiden-specific metrics 
		        String line1 = "=== Aggregate results over " + totalRuns + " runs, for configuration: " +
		            "#PR=" + cfg.numberPR +
		            ", reachability%=" + cfg.reach * 100 +
		            ", completeReachability%=" + cfg.complete * 100 +
		            ", portSpecific%=" + cfg.port * 100 + " ===";
		        System.out.println(line1);
	        if (resFile != null) {
	            resFile.write(line1 + "\n");
	            resFile.flush();
	        }
		        String line2 = String.format("SAT: %d  UNSAT: %d  ERR: %d", nSAT, nUNSAT, err);
		        System.out.println(line2);
	        if (resFile != null) {
	            resFile.write(line2 + "\n");
	            resFile.flush();
	        }
		        String line3 = String.format("Avg time: %.2f ms", avgTime);
		        System.out.println(line3);
	        if (resFile != null) {
	            resFile.write(line3 + "\n");
	            resFile.flush();
	        }
		        String line4 = String.format("Avg nodes: %.2f  Avg properties: %.2f  Avg firewalls: %.2f  Avg rules: %.2f",
		            avgNodes, avgPolicies, avgFW, avgRules);
		        System.out.println(line4);
	        if (resFile != null) {
	            resFile.write(line4 + "\n");
	            resFile.flush();
	        }
		        String line5 = String.format("Avg MaxSMT: %.2f  Avg Clustering: %.2f  Avg Merge: %.2f", avgMaxSMT, avgClustering, avgMerge);
		        System.out.println(line5);
	        if (resFile != null) {
	            resFile.write(line5 + "\n");
	            resFile.flush();
	        }
	        
	        if (resFile != null) {
	            resFile.write("\n");
	            resFile.flush();
	        }
		    }

		} catch (Exception e) {
		    e.printStackTrace();
		    fail(e.toString());
		} finally {
			if (resFile != null) {
				try {
					resFile.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	*/
	
	// Use this for testing without Leiden (just runs the tests without writing on file)
public static void testScalabilityPerformance() {
    rand = new Random(seed);
    Runtime rt = Runtime.getRuntime();
    long totalMem = rt.totalMemory();
    long maxMem = rt.maxMemory();
    long freeMem = rt.freeMemory();
    double megs = 1048576.0;

    long sumTime = 0;
    long sumNodes = 0;
    long sumPolicies = 0;
    long sumFW = 0;
    long sumRules = 0;
    long sumMaxSMT = 0;

    int totalRuns = runs;
    List<TestParams> configs = new ArrayList<>();
    for (int pr : numberPRs) {
        for (double[] pair : percentPairs) {
            for (double port : portSpecifics) {
                configs.add(new TestParams(pr, pair[0], pair[1], port));
            }
        }
    }

    System.out.println("Total Memory: " + totalMem + " (" + (totalMem / megs) + " MiB)");
    System.out.println("Max Memory:   " + maxMem + " (" + (maxMem / megs) + " MiB)");
    System.out.println("Free Memory:  " + freeMem + " (" + (freeMem / megs) + " MiB)");

    int[] seeds = new int[runs];
    for (int m = 0; m < runs; m++) {
        seeds[m] = Math.abs(rand.nextInt());
    }

    BufferedWriter resFile = null;
    try {
        resFile = Files.newBufferedWriter(Paths.get("results_stanford.log"));
    } catch (Exception e) {
        e.printStackTrace();
    }

    int k = 0;
    try {
        for (TestParams cfg : configs) {

            sumTime = 0;
            sumNodes = 0;
            sumPolicies = 0;
            sumFW = 0;
            sumRules = 0;
            sumMaxSMT = 0;

            for (k = 0; k < runs; k++) {
                TestCaseGeneratorStanford f = new TestCaseGeneratorStanford(
                        prefix + cfg.numberPR + "PR_r" + (cfg.reach)
                                + "_c" + (cfg.complete)
                                + "_p" + (cfg.port),
                        cfg.numberPR,
                        seed,
                        cfg.reach,
                        cfg.complete,
                        cfg.port
                );

                totTime = 0;
                maxTotTime = 0;
                minTotTime = Integer.MAX_VALUE;
                JAXBContext jc = JAXBContext.newInstance("it.polito.verefoo.jaxb");
                Unmarshaller u = jc.createUnmarshaller();
                SchemaFactory sf = SchemaFactory.newInstance(
                        XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = sf.newSchema(
                        new File("./xsd/nfvSchema.xsd"));
                u.setSchema(schema);
                Marshaller m = jc.createMarshaller();
                m.setProperty(
                        Marshaller.JAXB_FORMATTED_OUTPUT,
                        Boolean.TRUE);
                m.setProperty(
                        Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                        "./xsd/nfvSchema.xsd");
                try {
                    m = jc.createMarshaller();
                    m.setProperty(
                            Marshaller.JAXB_FORMATTED_OUTPUT,
                            Boolean.TRUE);
                    m.setProperty(
                            Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                            "./xsd/nfvSchema.xsd");

                    root = f.getNfv();
                    long t0 = System.currentTimeMillis();
                    NFV resultNFV = testCoarse(root);
                    long elapsed =
                            System.currentTimeMillis() - t0;
                    JAXBContext context =
                            JAXBContext.newInstance(
                                    "it.polito.verefoo.jaxb");

                    Marshaller marshaller =
                            context.createMarshaller();

                    marshaller.setProperty(
                            Marshaller.JAXB_FORMATTED_OUTPUT,
                            Boolean.TRUE);

                    RunStats stats =
                            analyzeNFV(resultNFV, m);

                    stats.elapsedMs = elapsed;

                    sumTime += stats.elapsedMs;
                    sumNodes += stats.nodes;
                    sumPolicies += stats.policies;
                    sumFW += stats.firewalls;
                    sumRules += stats.rules;
                    sumMaxSMT += totMaxSMTTime;

                    String runDetail = String.format(
                            "Run %d: time=%dms nodes=%d policies=%d fw=%d rules=%d maxSMT=%dms",
                            k,
                            stats.elapsedMs,
                            stats.nodes,
                            stats.policies,
                            stats.firewalls,
                            stats.rules,
                            totMaxSMTTime
                    );

                    System.out.println(runDetail);

                    if (resFile != null) {
                        resFile.write(runDetail + "\n");
                        resFile.flush();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    err++;
                }
            }

            double avgTime =
                    totalRuns > 0 ? ((double) sumTime) / totalRuns : 0;
            double avgNodes =
                    totalRuns > 0 ? ((double) sumNodes) / totalRuns : 0;
            double avgPolicies =
                    totalRuns > 0 ? ((double) sumPolicies) / totalRuns : 0;
            double avgFW =
                    totalRuns > 0 ? ((double) sumFW) / totalRuns : 0;
            double avgRules =
                    totalRuns > 0 ? ((double) sumRules) / totalRuns : 0;
            double avgMaxSMT =
                    totalRuns > 0 ? ((double) sumMaxSMT) / totalRuns : 0;
            String line1 =
                    "=== Aggregate results over "
                            + totalRuns
                            + " runs, for configuration: "
                            + "#PR=" + cfg.numberPR
                            + ", reachability%=" + cfg.reach * 100
                            + ", completeReachability%=" + cfg.complete * 100
                            + ", portSpecific%=" + cfg.port * 100
                            + " ===";

            System.out.println(line1);

            if (resFile != null) {
                resFile.write(line1 + "\n");
                resFile.flush();
            }

            String line2 = String.format(
                    "SAT: %d  UNSAT: %d  ERR: %d",
                    nSAT,
                    nUNSAT,
                    err
            );
            System.out.println(line2);
            if (resFile != null) {
                resFile.write(line2 + "\n");
                resFile.flush();
            }

            String line3 =
                    String.format(
                            "Avg time: %.2f ms",
                            avgTime);

            System.out.println(line3);

            if (resFile != null) {
                resFile.write(line3 + "\n");
                resFile.flush();
            }

            String line4 =
                    String.format(
                            "Avg nodes: %.2f  Avg properties: %.2f  Avg firewalls: %.2f  Avg rules: %.2f",
                            avgNodes,
                            avgPolicies,
                            avgFW,
                            avgRules
                    );

            System.out.println(line4);

            if (resFile != null) {
                resFile.write(line4 + "\n");
                resFile.flush();
            }

            String line5 =
                    String.format(
                            "Avg MaxSMT: %.2f",
                            avgMaxSMT
                    );

            System.out.println(line5);

            if (resFile != null) {
                resFile.write(line5 + "\n");
                resFile.flush();
                resFile.write("\n");
                resFile.flush();
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        fail(e.toString());

    } finally {
        if (resFile != null) {
            try {
                resFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
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
                            if (n.getFunctionalType() == FunctionalTypes.FIREWALL){
                                numFW++;
                                if (n.getConfiguration().getFirewall() != null && n.getConfiguration().getFirewall().getElements() != null 
										&& !n.getConfiguration().getFirewall().getElements().isEmpty()) {
									numRules += (int) n.getConfiguration().getFirewall().getElements().size();
                                }
								else {
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
