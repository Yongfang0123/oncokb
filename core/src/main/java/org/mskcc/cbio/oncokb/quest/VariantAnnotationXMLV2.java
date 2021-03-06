/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mskcc.cbio.oncokb.quest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.mskcc.cbio.oncokb.bo.GeneBo;
import org.mskcc.cbio.oncokb.model.Alteration;
import org.mskcc.cbio.oncokb.model.AlterationType;
import org.mskcc.cbio.oncokb.model.Gene;
import org.mskcc.cbio.oncokb.util.AlterationUtils;
import org.mskcc.cbio.oncokb.util.ApplicationContextSingleton;
import org.mskcc.cbio.oncokb.util.FileUtils;
import org.mskcc.cbio.oncokb.util.GeneUtils;
import org.mskcc.cbio.oncokb.util.TumorTypeUtils;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author jgao
 */
@Controller
public final class VariantAnnotationXMLV2 {
    
    public static void main(String[] args) throws FileNotFoundException {
        String input = args[0];
        InputStream is = new FileInputStream(input);
        String annotation = getVariantAnnotation(is);
        
        String output;
        if (args.length>1) {
            output = args[1];
        } else {
            output = input.substring(0, input.length()-4)+".oncokb.xml";
        }
        
        try (PrintWriter writer = new PrintWriter(output)) {
            writer.append(annotation);
        }
    }
    
    public static String getVariantAnnotation(InputStream isXml) {
        try {
            Map<Alteration, String> mapAlterationXml = new LinkedHashMap<Alteration, String>();

            SAXReader reader = new SAXReader();
            Document document = reader.read(isXml);

            // sample id
            String sampleId = document.selectSingleNode("//xml/sample_id").getText();

            // test id
            String testId = document.selectSingleNode("//xml/test_id").getText();

            // diagnosis
            String diagnosis = document.selectSingleNode("//xml/diagnosis").getText();

            // Somatic variants
            List<Node> vcfNodes = document.selectNodes("//xml/somatic_variants/data");
            if (!vcfNodes.isEmpty()) {
                runVcf2Maf(document.asXML(), mapAlterationXml, diagnosis);
            }
//                for (Node vcfNode : vcfNodes) {
//                    handleSomaticVariants(vcfNode, alterations, alterationXmls, diagnosis);
//                }

            // Copy number alterations
            List<Node> cnaNodes = document.selectNodes("//xml/copy_number_alterations/copy_number_alteration");
            for (Node cnaNode : cnaNodes) {
                handleCNA(cnaNode, mapAlterationXml, diagnosis);
            }

            // fusions
            List<Node> fusionNodes = document.selectNodes("//xml/fusion_genes/fusion_gene");
            for (Node fusionNode : fusionNodes) {
                handleFusion(fusionNode, mapAlterationXml, diagnosis);
            }

            List<Node> entrezNodes = document.selectNodes("//xml/sequencing_panel/genes/gene/entrez_gene_id");
            ArrayList<Gene> sequencedGenes = new ArrayList<Gene>();
            //use entrezGeneID, which is more stable than hugoSymbol, to fetch Gene instance
            for(int i = 0;i < entrezNodes.size();i++){
                  sequencedGenes.add(GeneUtils.getGeneByEntrezId(Integer.parseInt(entrezNodes.get(i).getText())));
            }
            // identify other related variants such as KRAS wildtype
            identifyAdditionalVariants(mapAlterationXml, diagnosis, sequencedGenes);

            List<Alteration> alterations = getAlterationOrderByLevelOfEvidence(mapAlterationXml);

            // exporting
            StringBuilder sb = new StringBuilder();

            // meta
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
            sb.append("<document>\n");
            sb.append("<date_generated>2015-01-09</date_generated>\n");
            sb.append("<oncokb_api_version>0.2</oncokb_api_version>\n");
            sb.append("<ensembl_version>79</ensembl_version>\n");
            sb.append("<sample>\n");
            sb.append("<sample_id>").append(sampleId).append("</sample_id>\n");
            sb.append("<diagnosis>").append(diagnosis).append("</diagnosis>\n");
            sb.append("<test>\n");
            sb.append("<test_id>").append(testId).append("</test_id>\n");

            // variants
            sb.append(exportVariants(alterations, mapAlterationXml));

            // variant_interation

            // end
            sb.append("</test>\n");
            sb.append("</sample>\n");
            sb.append("</document>");

            return sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "<document>Failed to upload file.</document>";
        }
    }
    
    private static void handleSomaticVariants(Node vcfNode, Map<Alteration, String> mapAlterationXml, String diagnosis) {
        // do it here
    }
    
    /**
     * This is a hacky way to run VEP. We should switch to web service once that is ready.
     */
    private static void runVcf2Maf(String inputXml, Map<Alteration, String> mapAlterationXml, String diagnosis) throws IOException, DocumentException, InterruptedException {
        File tmpFile = File.createTempFile("temp-oncokb-input-", ".xml");
        tmpFile.deleteOnExit();
        String inputPath = tmpFile.getAbsolutePath();
        String outputPath = inputPath.substring(0,inputPath.length()-3) + "oncokb.xml";
        
        FileWriter writer = new FileWriter(tmpFile);
        writer.append(inputXml);
        writer.close();
        
        String vepMafXmlPl = System.getenv("VEP_MAF_XML_PL");
        if (null==vepMafXmlPl) {
            throw new IOException("VEP_MAF_XML_PL was not defined");
        }
        
        Process proc = Runtime.getRuntime().exec(new String[]{"perl",vepMafXmlPl,inputPath});
        proc.waitFor();
        
        InputStream stderr = proc.getErrorStream();
        InputStreamReader isr = new InputStreamReader(stderr);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        System.out.println("<ERROR>");
        while ( (line = br.readLine()) != null)
            System.out.println(line);
        System.out.println("</ERROR>");
        int exitVal = proc.waitFor();
        System.out.println("Process exitValue: " + exitVal);
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(outputPath);
        
        List<Node> variantNodes = document.selectNodes("//document/sample/test/variant");
        for (Node node : variantNodes) {
            String alterationXml = "<variant_type>small_nucleotide_variant</variant_type>\n"
                    + node.selectSingleNode("genomic_locus").asXML()
                    + node.selectSingleNode("allele").asXML();
            
            String geneSymbol = node.selectSingleNode("allele/transcript/hgnc_symbol").getText();
            GeneBo geneBo = ApplicationContextSingleton.getGeneBo();
            Gene gene = geneBo.findGeneByHugoSymbol(geneSymbol);
            
            String proteinChange = node.selectSingleNode("allele/transcript/hgvs_p_short").getText();
            
            Alteration alteration = new Alteration();
            alteration.setAlterationType(AlterationType.MUTATION);
            alteration.setGene(gene);
            alteration.setName(proteinChange);
            
            AlterationUtils.annotateAlteration(alteration, proteinChange);
            
            mapAlterationXml.put(alteration, alterationXml);
        }

    }
    
    private static void handleCNA(Node cnaNode, Map<Alteration, String> mapAlterationXml, String diagnosis) {
        Gene gene = parseGene(cnaNode, "gene");
        if (gene==null) {
            return;
        }
        
        String type = cnaNode.selectSingleNode("type").getText();
        
        Alteration alteration = new Alteration();
        alteration.setGene(gene);
        alteration.setAlteration(type);
        alteration.setAlterationType(AlterationType.MUTATION); // TODO: this needs to be fixed
        
        StringBuilder sb = new StringBuilder();
        sb.append("<variant_type>copy_number_alteration</variant_type>\n");
        sb.append(cnaNode.asXML()).append("\n");
        sb.append(VariantAnnotationXML.annotate(alteration, diagnosis));
        
        mapAlterationXml.put(alteration, sb.toString());
    }
    
    private static void handleFusion(Node fusionNode, Map<Alteration, String> mapAlterationXml, String diagnosis) {
        StringBuilder sb = new StringBuilder();
        sb.append("<variant_type>fusion_gene</variant_type>\n");
        sb.append(fusionNode.asXML()).append("\n");

        Gene gene1 = parseGene(fusionNode, "gene_1");
        Gene gene2 = parseGene(fusionNode, "gene_2");
        
//        String type = fusionNode.selectSingleNode("type").getText();
        
        String fusion = gene1.getHugoSymbol()+"-"+gene2.getHugoSymbol()+" fusion";
        
        Alteration alteration = new Alteration();
        alteration.setGene(gene2);
        alteration.setAlteration(fusion);
        alteration.setAlterationType(AlterationType.MUTATION); // TODO: this needs to be fixed
        
        sb.append(VariantAnnotationXML.annotate(alteration, diagnosis));
        
        mapAlterationXml.put(alteration, sb.toString());

    }

    private static void identifyAdditionalVariants(Map<Alteration, String> mapAlterationXml, String diagnosis, ArrayList<Gene> sequencedGenes) {
        List<String> lines;
        try {
            ArrayList<Alteration> alterations = new ArrayList<Alteration>(mapAlterationXml.keySet());
            ArrayList<Gene> genesWithMutation = new ArrayList<Gene>();
            for(int i = 0;i < alterations.size();i++){
                genesWithMutation.add(alterations.get(i).getGene());
            }
            lines = FileUtils.readTrimedLinesStream(TumorTypeUtils.class.getResourceAsStream("/data/wildtype-alterations.txt"));
            String[] parts = new String[2];
            Alteration tempAlteration = new Alteration();
            Gene tempGene = new Gene();
            for (String line : lines) {
                if (line.startsWith("#")) {
                   continue;
                }
                parts = line.split("\t");
                tempGene = GeneUtils.getGeneByHugoSymbol(parts[0]);
                if(sequencedGenes.contains(tempGene) && diagnosis.equalsIgnoreCase(parts[1]) && !genesWithMutation.contains(tempGene)){
                    tempAlteration.setGene(tempGene);
                    tempAlteration.setAlteration("Wildtype");
                    tempAlteration.setAlterationType(AlterationType.MUTATION); 
                    StringBuilder sb = new StringBuilder();
                    sb.append("<variant_type>wildtype</variant_type>\n");
                    sb.append(VariantAnnotationXML.annotate(tempAlteration, diagnosis));

                    mapAlterationXml.put(tempAlteration, sb.toString());
                }
         
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       
    }
    
    private static List<Alteration> getAlterationOrderByLevelOfEvidence(Map<Alteration, String> mapAlterationXml) {
        //sort the variant based on the highest level in the order of: 0 > 1 > R1 > 2A > 2B > R2 > 3A > 3B > R3 > 4
        String[] levels = {"0", "1", "R1", "2A", "2B", "R2", "3A", "3B", "R3", "4"};
        ArrayList<Alteration> alterations = new ArrayList<Alteration>(mapAlterationXml.keySet());
        final Map<Alteration, Integer> mapAlterationLevel = new LinkedHashMap<Alteration, Integer>();
        String relevant = "", tempLevel = "";
        Integer levelIndex, lowestLevelIndex;
        for(int i = 0;i < alterations.size();i++){
            levelIndex = -1;
            lowestLevelIndex = -1;
            String tempXMLString = "<xml>" + mapAlterationXml.get(alterations.get(i)) + "</xml>";
            try  
            {  
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource src = new InputSource();
                src.setCharacterStream(new StringReader(tempXMLString));
                org.w3c.dom.Document doc = builder.parse(src);
                NodeList cancerTypeNodes = doc.getElementsByTagName("cancer_type");
                for(int j = 0;j < cancerTypeNodes.getLength();j++){
                    Element currentNode = (Element)cancerTypeNodes.item(j);
                    relevant = currentNode.getAttributes().getNamedItem("relevant_to_patient_disease").getNodeValue();
                    NodeList levelNodes = currentNode.getElementsByTagName("level");
                    if(relevant.equalsIgnoreCase("Yes") && currentNode.getElementsByTagName("level").getLength() > 0){
                        levelIndex = 100;
                        for(int k = 0; k < levelNodes.getLength();k++){
                            tempLevel = levelNodes.item(k).getTextContent().toUpperCase();
                            lowestLevelIndex = ArrayUtils.indexOf(levels, tempLevel);
                            if(lowestLevelIndex < levelIndex)levelIndex = lowestLevelIndex;
                        }
                    }
                }
                mapAlterationLevel.put(alterations.get(i), levelIndex);
            } catch (Exception e) {  
                e.printStackTrace();  
            } 
           
        }
        Collections.sort(alterations, new Comparator<Alteration>() {
            public int compare(Alteration a1, Alteration a2) {
                Integer aLevel = mapAlterationLevel.get(a1), bLevel = mapAlterationLevel.get(a2);
                if(aLevel == -1)return 1;
                if(bLevel == -1)return -1;
                return aLevel - bLevel; 
            }
        });

        return alterations;
    }
    
    private static Gene parseGene(Node node, String genePath) {
        GeneBo geneBo = ApplicationContextSingleton.getGeneBo();
        Gene gene = null;
        Node entrezGeneIdNode = node.selectSingleNode(genePath+"/entrez_gene_id");
        Node hugoSymbolNode = node.selectSingleNode(genePath+"/hgnc_symbol");
        if (entrezGeneIdNode!=null) {
            int entrezId = Integer.parseInt(entrezGeneIdNode.getText());
            gene = geneBo.findGeneByEntrezGeneId(entrezId);
        }
        
        if (gene==null && hugoSymbolNode!=null) {
            String symbol = hugoSymbolNode.getText();
            gene = geneBo.findGeneByHugoSymbol(symbol);
        }
        
        if (gene==null) {
            // a gene not in system
            gene = new Gene();
            gene.setEntrezGeneId(Integer.parseInt(entrezGeneIdNode.getText()));
            gene.setHugoSymbol(hugoSymbolNode.getText());
        }
        
        return gene;
    }
    
    private static String exportVariants(List<Alteration> alterations, Map<Alteration, String> mapAlterationXml) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<alterations.size(); i++) {
            Alteration alt = alterations.get(i);
            String xml = mapAlterationXml.get(alt);
            sb.append("<variant no=\"").append(i+1).append("\">\n").append(xml).append("</variant>\n");
        }
        return sb.toString();
    }
    
}
