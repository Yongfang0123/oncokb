package org.mskcc.cbio.oncokb.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mskcc.cbio.oncokb.model.Alteration;
import org.mskcc.cbio.oncokb.model.AlterationType;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by Hongxin on 12/23/16.
 */

@RunWith(Parameterized.class)
public class FindRelevantAlterationsTest {
    private String hugoSymbol;
    private String alteration;
    private String alterationType;
    private String expectedRelevantAlterations;

    public FindRelevantAlterationsTest(String hugoSymbol, String alteration, String alterationType, String expectedRelevantAlterations) {
        this.hugoSymbol = hugoSymbol;
        this.alteration = alteration;
        this.alterationType = alterationType;
        this.expectedRelevantAlterations = expectedRelevantAlterations;
    }

    @Parameterized.Parameters
    public static Collection<String[]> getParameters() {
        return Arrays.asList(
            new String[][]{
                // Critical cases
                {"BRAF", "V600E", null, "V600E, V600A, V600D, V600G, V600K, V600L, V600M, V600Q, V600R, VK600EI, Oncogenic Mutations"},

                // Check Fusions
                {"BRAF", "PAPSS1-BRAF Fusion", null, "PAPSS1-BRAF Fusion, Fusions, Oncogenic Mutations"},
                {"CTCF", "CTCF-intragenic", null, "Truncating Mutations"},
                {"CTCF", "CTCF intragenic", null, "Truncating Mutations"},
                {"CTCF", "Intragenic", null, "Truncating Mutations"},
                {"NOTCH1", "NOTCH1-intragenic", null, "Fusions, Gain-of-function Mutations"},

                // Tumor suppressor should be mapped with Truncating Mutations. (The code does not check whether gene
                // is tumor suppressor, just check whether Fusions is curated, is not, link Truncating Mutations)
                {"PIK3R1", "KCTD16-PIK3R1 fusion", null, "KCTD16-PIK3R1 fusion, Truncating Mutations, Oncogenic Mutations"},

                // General truncating consequence should be associated with Truncating Mutations
                // Check splice
                {"TP53", "X33_splice", null, "X33_splice, Truncating Mutations, Oncogenic Mutations"},
                // Check stop_gained
                {"MAP2K4", "R304*", null, "R304*, Truncating Mutations, Oncogenic Mutations"},

                // EGFR exon deletion
                {"EGFR", "vIII", null, "vIII, Oncogenic Mutations"},
                {"EGFR", "CTD", null, "G983_A1210indel, Oncogenic Mutations"},
                {"EGFR", "vIV", null, "G983_A1210indel, Oncogenic Mutations"},
                {"EGFR", "vIVa", null, "G983_A1210indel, Oncogenic Mutations"},
                {"EGFR", "vIVb", null, "G983_A1210indel, Oncogenic Mutations"},
                {"EGFR", "vIVc", null, "G983_A1210indel, Oncogenic Mutations"},

                // Check range
                {"MED12", "G44S", null, "G44S, G44A, G44C, G44D, G44V, 34_68mut"},
                {"MED12", "G44D", null, "G44D, G44A, G44C, G44S, G44V, 34_68mut"},
                {"NOTCH1", "Q2405Rfs*17", null, "Q2405Rfs*17, T2375_K2555trunc, Gain-of-function Mutations"},

                // VUS should get mapped to hotspot VUS, but should not get Oncogenic Mutations from the hotspot VUS.
                // In this case VUS N109_R113del is covered by VUS I99_R113del, and I99_R113del is a hotpot.
                {"MAP2K1", "N109_R113del", null, "N109_R113del, I99_R113del"},

                // Range missense variant
                {"PDGFRA", "D842I", null, "D842I, D842H, D842V, D842Y, D842_I843delinsIM, Oncogenic Mutations"},

                // Truncating Mutations in the Oncogene should not be mapped to any range mutation unless the consequence is truncating
                {"KIT", "K509Nfs*2", null, ""},
                {"MED12", "Q1836Lfs*57", null, "Truncating Mutations"},
                {"PIK3CA", "*1069Ffs*5", null, ""},

                // 34 is in Exon 2, the Exon 2 should not be mapped.
                {"MED12", "A34*", null, "Truncating Mutations"},

                {"NOTCH1", "Q2405Rfs*17", null, "Q2405Rfs*17, T2375_K2555trunc, Gain-of-function Mutations"},

                // Deletion
                // With specific Deletion curated
                {"BRCA2", "Deletion", null, "Deletion, Oncogenic Mutations"},
                // Without specific Deletion curated
                {"MAP2K4", "Deletion", null, "Truncating Mutations, Oncogenic Mutations"},
            });
    }

    private String toString(LinkedHashSet<Alteration> relevantAlterations) {
        List<String> names = new ArrayList<>();
        for (Alteration alteration : relevantAlterations) {
            names.add(alteration.getAlteration());
        }
        return MainUtils.listToString(names, ", ");
    }

    @Test
    public void testAnnotateAlteration() throws Exception {
        // Particularly test consequence
        Alteration alt = AlterationUtils.getAlteration(hugoSymbol, alteration, AlterationType.getByName(alterationType), null, null, null);
        AlterationUtils.annotateAlteration(alt, alteration);

        LinkedHashSet<Alteration> relevantAlterations =
            ApplicationContextSingleton.getAlterationBo()
                .findRelevantAlterations(alt, AlterationUtils.getAllAlterations(alt.getGene()), true);
        String relevantAltsName = toString(relevantAlterations);

        assertEquals("Relevant alterations are not matched on case " +
            hugoSymbol + " " + alteration + " " + alterationType + " ", expectedRelevantAlterations, relevantAltsName);
    }

}
