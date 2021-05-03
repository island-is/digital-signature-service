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
        TLValidationJobSummary summary = job.getSummary();
        List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
        if (!isProcessed(lotlInfos)) {
            return false;
        }
        for (LOTLInfo lotlInfo : lotlInfos) {
            if (!isProcessed(lotlInfo.getPivotInfos())) {
                return false;
            }
            if (!isProcessed(lotlInfo.getTLInfos())) {
                return false;
            }
        }
        if (!isProcessed(summary.getOtherTLInfos())) {
            return false;
        }
        return true;
    }

    private <T extends TLInfo> boolean isProcessed(List<T> tlInfos) {
        for (T tlInfo : tlInfos) {
            if (!isProcessed(tlInfo.getDownloadCacheInfo())) {
                return false;
            }
        }
        return true;
    }

    private boolean isProcessed(InfoRecord record) {
        // REFRESH_NEEDED on download task means the refresh() method has not been called yet
        // DESYNCHRONIZED on download means the job has been interrupted in the middle and is not finished
        // SYNCHRONIZED, ERROR, TO_BE_DELETED represent the final task cache states
        return record.isSynchronized() || record.isError() || record.isToBeDeleted();
    }

    /**
     * This method verifies if all LOTL/TLs are available and have been downloaded successfully
     *
     * @return TRUE if all LOTL/TLs are available, FALSE otherwise
     */
    @RequestMapping(value = "/tls-available", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public Boolean areTLsAvailable() {
        TLValidationJobSummary summary = job.getSummary();
        List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
        if (!isAvailable(lotlInfos)) {
            return false;
        }
        for (LOTLInfo lotlInfo : lotlInfos) {
            if (!isAvailable(lotlInfo.getPivotInfos())) {
                return false;
            }
            if (!isAvailable(lotlInfo.getTLInfos())) {
                return false;
            }
        }
        if (!isAvailable(summary.getOtherTLInfos())) {
            return false;
        }
        return true;
    }

    private <T extends TLInfo> boolean isAvailable(List<T> tlInfos) {
        for (T tlInfo : tlInfos) {
            if (!tlInfo.getDownloadCacheInfo().isResultExist() || !isSynchronized(tlInfo.getDownloadCacheInfo())) {
                return false;
            }
        }
        return true;
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
        // TO_BE_DELETED indicated that a URL of a TL has been changed, but not an error
        return record.isSynchronized() || record.isToBeDeleted();
    }

}
