package org.mskcc.cbio.oncokb.dao;

import java.util.List;
import org.mskcc.cbio.oncokb.model.ClinicalTrial;
import org.mskcc.cbio.oncokb.model.Drug;
import org.mskcc.cbio.oncokb.model.TumorType;



/**
 *
 * @author jgao
 */
public interface ClinicalTrialDao extends GenericDao<ClinicalTrial, Integer> {
    /**
     * 
     * @param nctId
     * @return 
     */
    ClinicalTrial findClinicalTrialByNctId(String nctId);
    
    /**
     * 
     * @param tumorType
     * @param drug
     * @return 
     */
    List<ClinicalTrial> findClinicalTrialByTumorTypeAndDrug(TumorType tumorType, Drug drug);
}
