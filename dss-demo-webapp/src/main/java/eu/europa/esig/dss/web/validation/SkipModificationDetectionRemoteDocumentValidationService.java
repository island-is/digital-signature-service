package eu.europa.esig.dss.web.validation;

import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.web.service.ValidationService;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.ws.validation.dto.DataToValidateDTO;

public class SkipModificationDetectionRemoteDocumentValidationService extends RemoteDocumentValidationService {

    public SkipModificationDetectionRemoteDocumentValidationService() {
        super();
    }

    @Override
    protected SignedDocumentValidator initValidator(DataToValidateDTO dataToValidate) {
        SignedDocumentValidator signedDocValidator = super.initValidator(dataToValidate);
        ValidationService.configureDocumentValidator(signedDocValidator);
        return signedDocValidator;
    }

}
