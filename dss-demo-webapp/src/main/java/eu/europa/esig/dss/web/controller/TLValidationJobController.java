package eu.europa.esig.dss.web.controller;

import eu.europa.esig.dss.spi.tsl.InfoRecord;
import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = "/tl-validation-job")
public class TLValidationJobController {

    @Autowired
    private TLValidationJob job;

    /**
     * This method verifies if TLValidationJob has been finished (summary is accessible)
     *
     * @return TRUE if the TLValidationJob has been finished, FALSE otherwise
     */
    @RequestMapping(value = "/ready", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public Boolean isReady() {
        return job.getSummary() != null;
    }

    /**
     * This method verifies if all LOTL/TLs have been processed and all of the entries are valid
     * (download, parsing and validation passed)
     *
     * @return TRUE if the TLValidationJob if all LOTL/TLs are valid, FALSE otherwise
     */
    @RequestMapping(value = "/valid", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public Boolean isValid() {
        TLValidationJobSummary summary = job.getSummary();
        List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
        if (!isValid(lotlInfos)) {
            return false;
        }
        for (LOTLInfo lotlInfo : lotlInfos) {
            if (!isValid(lotlInfo.getPivotInfos())) {
                return false;
            }
            if (!isValid(lotlInfo.getTLInfos())) {
                return false;
            }
        }
        if (!isValid(summary.getOtherTLInfos())) {
            return false;
        }
        return true;
    }

    private <T extends TLInfo> boolean isValid(List<T> tlInfos) {
        for (T tlInfo : tlInfos) {
            if (!isSynchronized(tlInfo.getDownloadCacheInfo()) || !isSynchronized(tlInfo.getParsingCacheInfo()) ||
                    !isSynchronized(tlInfo.getValidationCacheInfo()) || !tlInfo.getValidationCacheInfo().isValid()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSynchronized(InfoRecord record) {
        return record.isSynchronized();
    }

}
