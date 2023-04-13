package eu.europa.esig.dss.web.validation;

import eu.europa.esig.dss.exception.IllegalInputException;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.policy.ValidationPolicy;
import eu.europa.esig.dss.policy.ValidationPolicyFacade;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.web.service.ValidationService;
import eu.europa.esig.dss.ws.converter.RemoteDocumentConverter;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.ws.validation.dto.DataToValidateDTO;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

// TODO: temporary solution, after 5.12 override #initValidator method only
public class SkipVisualValidationRemoteDocumentValidationService extends RemoteDocumentValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SkipVisualValidationRemoteDocumentValidationService.class);

    private CertificateVerifier verifier;
    private ValidationPolicy defaultValidationPolicy;

    public SkipVisualValidationRemoteDocumentValidationService() {
        // empty
    }

    public void setVerifier(CertificateVerifier verifier) {
        super.setVerifier(verifier);
        this.verifier = verifier;
    }

    @Override
    public void setDefaultValidationPolicy(ValidationPolicy defaultValidationPolicy) {
        super.setDefaultValidationPolicy(defaultValidationPolicy);
        this.defaultValidationPolicy = defaultValidationPolicy;
    }

    @Override
    public WSReportsDTO validateDocument(DataToValidateDTO dataToValidate) {
        LOG.info("ValidateDocument in process...");
        SignedDocumentValidator validator = this.initValidator(dataToValidate);
        RemoteDocument policy = dataToValidate.getPolicy();
        Reports reports;
        if (policy != null) {
            reports = validator.validateDocument(this.getValidationPolicy(policy));
        } else if (this.defaultValidationPolicy != null) {
            reports = validator.validateDocument(this.defaultValidationPolicy);
        } else {
            reports = validator.validateDocument();
        }

        WSReportsDTO reportsDTO = new WSReportsDTO(reports.getDiagnosticDataJaxb(), reports.getSimpleReportJaxb(), reports.getDetailedReportJaxb(), reports.getEtsiValidationReportJaxb());
        LOG.info("ValidateDocument is finished");
        return reportsDTO;
    }

    private ValidationPolicy getValidationPolicy(RemoteDocument policy) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(policy.getBytes())) {
            return ValidationPolicyFacade.newFacade().getValidationPolicy(bais);
        } catch (Exception e) {
            throw new IllegalInputException(String.format("Unable to load the validation policy : %s", e.getMessage()), e);
        }
    }

    private SignedDocumentValidator initValidator(DataToValidateDTO dataToValidate) {
        DSSDocument signedDocument = RemoteDocumentConverter.toDSSDocument(dataToValidate.getSignedDocument());
        SignedDocumentValidator signedDocValidator = SignedDocumentValidator.fromDocument(signedDocument);
        if (Utils.isCollectionNotEmpty(dataToValidate.getOriginalDocuments())) {
            signedDocValidator.setDetachedContents(RemoteDocumentConverter.toDSSDocuments(dataToValidate.getOriginalDocuments()));
        }

        signedDocValidator.setCertificateVerifier(this.verifier);
        if (dataToValidate.getTokenExtractionStrategy() != null) {
            signedDocValidator.setTokenExtractionStrategy(dataToValidate.getTokenExtractionStrategy());
        }

        ValidationService.configureDocumentValidator(signedDocValidator);

        return signedDocValidator;
    }

}
