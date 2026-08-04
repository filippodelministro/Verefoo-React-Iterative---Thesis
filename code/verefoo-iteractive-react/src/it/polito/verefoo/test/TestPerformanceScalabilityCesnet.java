package it.polito.verefoo.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import it.polito.verefoo.VerefooSerializer;
import it.polito.verefoo.extra.TestCaseGeneratorCesnet;
import it.polito.verefoo.jaxb.NFV;

public class TestPerformanceScalabilityCesnet {

    public static void main(String[] args) {
        String fileName = "testfile/CESNET/cesnet1";
        Boolean isolationBidirectional = true;
        Boolean usePorts = true;
        int policyNumber = 3;
        Double reachabilityPerc = 0.05;
        
		for (int i = 0; i < 30; i++) {
			try {
                TestCaseGeneratorCesnet cesnet = new TestCaseGeneratorCesnet(fileName, isolationBidirectional, policyNumber, reachabilityPerc, usePorts);

                // JAXBContext e validation scheme creation
                JAXBContext jc = JAXBContext.newInstance("it.polito.verefoo.jaxb");
                Unmarshaller u = jc.createUnmarshaller();
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = sf.newSchema(new File("./xsd/nfvSchema.xsd"));
                u.setSchema(schema);

                // Marshaller to write the XML
                Marshaller m = jc.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "./xsd/nfvSchema.xsd");
                
        		System.out.println("===== TEST PERFORMANCE SCALABILITY CESNET ======");
        		System.out.println("\n---------------------------------------------------------");
                int j = i+1;
                System.out.println("Run: " + j);
                System.out.println("Policy Number: " + policyNumber);
                System.out.println("Reachability Percentage: " + reachabilityPerc);
                System.out.println("Use Ports: " + usePorts);
                System.out.println("\n");

                // Obtain the NFV object
                NFV root = cesnet.getNFV();
                
                // Debug
//                m.marshal(root, System.out);

                // Function to call the VerefooSerializer
                NFV resultNFV = testCoarse(root);
                System.out.println(resultNFV);
//                File outputFile = new File(fileName + ".xml");
//                if (resultNFV != null) {
//                    m.marshal(resultNFV, outputFile);
//                } else {
//                    m.marshal(root, outputFile);
//                }
//
//                System.out.println("XML salvato in: " + outputFile.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
		}
    }

    /**
     * The function measures the execution time and check if the configuration is satisfiable.
     * 
     * @param root The NFV object containing the configuration of the network
     * @return The resulting NFV object after the test
     */
    private static NFV testCoarse(NFV root) throws Exception {
        long beginAll = System.currentTimeMillis();
        VerefooSerializer test = new VerefooSerializer(root, "AP", true);
        
        long endAll = System.currentTimeMillis();
        
        System.out.println("SAT - Time: " + (endAll - beginAll) + " ms");

        return test.getResult();
    }
}
