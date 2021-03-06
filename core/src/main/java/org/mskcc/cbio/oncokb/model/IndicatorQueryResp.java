package org.mskcc.cbio.oncokb.model;
// Generated Dec 19, 2013 1:33:26 AM by Hibernate Tools 3.2.1.GA

import org.mskcc.cbio.oncokb.apiModels.MutationEffectResp;

import java.util.ArrayList;
import java.util.List;


/**
 * TumorType generated by hbm2java
 */

public class IndicatorQueryResp implements java.io.Serializable {
    private Query query;
    private Boolean geneExist;
    private Boolean variantExist;
    private Boolean alleleExist;
    private String oncogenic;
    private MutationEffectResp mutationEffect;
    private LevelOfEvidence highestSensitiveLevel;
    private LevelOfEvidence highestResistanceLevel;
    private List<LevelOfEvidence> otherSignificantSensitiveLevels;
    private List<LevelOfEvidence> otherSignificantResistanceLevels;
    private Boolean VUS;
    private Boolean hotspot;
    private String geneSummary;
    private String variantSummary;
    private String tumorTypeSummary;
    private List<IndicatorQueryTreatment> treatments = new ArrayList<>();
    private String dataVersion;
    private String lastUpdate;

    public IndicatorQueryResp() {
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public Boolean getGeneExist() {
        return geneExist;
    }

    public void setGeneExist(Boolean geneExist) {
        this.geneExist = geneExist;
    }

    public Boolean getVariantExist() {
        return variantExist;
    }

    public void setVariantExist(Boolean variantExist) {
        this.variantExist = variantExist;
    }

    public String getOncogenic() {
        return oncogenic;
    }

    public void setOncogenic(String oncogenic) {
        this.oncogenic = oncogenic;
    }

    public MutationEffectResp getMutationEffect() {
        return mutationEffect;
    }

    public void setMutationEffect(MutationEffectResp mutationEffect) {
        this.mutationEffect = mutationEffect;
    }

    public LevelOfEvidence getHighestSensitiveLevel() {
        return highestSensitiveLevel;
    }

    public void setHighestSensitiveLevel(LevelOfEvidence highestSensitiveLevel) {
        this.highestSensitiveLevel = highestSensitiveLevel;
    }

    public LevelOfEvidence getHighestResistanceLevel() {
        return highestResistanceLevel;
    }

    public void setHighestResistanceLevel(LevelOfEvidence highestResistanceLevel) {
        this.highestResistanceLevel = highestResistanceLevel;
    }

    public List<LevelOfEvidence> getOtherSignificantSensitiveLevels() {
        return otherSignificantSensitiveLevels;
    }

    public void setOtherSignificantSensitiveLevels(List<LevelOfEvidence> otherSignificantSensitiveLevels) {
        this.otherSignificantSensitiveLevels = otherSignificantSensitiveLevels;
    }

    public List<LevelOfEvidence> getOtherSignificantResistanceLevels() {
        return otherSignificantResistanceLevels;
    }

    public void setOtherSignificantResistanceLevels(List<LevelOfEvidence> otherSignificantResistanceLevels) {
        this.otherSignificantResistanceLevels = otherSignificantResistanceLevels;
    }

    public Boolean getVUS() {
        return VUS;
    }

    public void setVUS(Boolean VUS) {
        this.VUS = VUS;
    }

    public Boolean getHotspot() {
        return hotspot;
    }

    public void setHotspot(Boolean hotspot) {
        this.hotspot = hotspot;
    }

    public Boolean getAlleleExist() {
        return alleleExist;
    }

    public void setAlleleExist(Boolean alleleExist) {
        this.alleleExist = alleleExist;
    }

    public String getGeneSummary() {
        return geneSummary;
    }

    public void setGeneSummary(String geneSummary) {
        this.geneSummary = geneSummary;
    }

    public String getVariantSummary() {
        return variantSummary;
    }

    public void setVariantSummary(String variantSummary) {
        this.variantSummary = variantSummary;
    }

    public String getTumorTypeSummary() {
        return tumorTypeSummary;
    }

    public void setTumorTypeSummary(String tumorTypeSummary) {
        this.tumorTypeSummary = tumorTypeSummary;
    }

    public List<IndicatorQueryTreatment> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<IndicatorQueryTreatment> treatments) {
        this.treatments = treatments;
    }

    public String getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(String dataVersion) {
        this.dataVersion = dataVersion;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}


