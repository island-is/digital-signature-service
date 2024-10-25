package eu.europa.esig.dss.web.job.sha2;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.tsl.sha2.DocumentWithSha2;

public class DocumentWithSha2Debug extends DocumentWithSha2 {

    /**
     * Default constructor
     *
     * @param document     {@link DSSDocument} original downloaded document
     * @param sha2Document {@link DSSDocument} corresponding sha2 document, containing digests of the {@code document}
     */
    protected DocumentWithSha2Debug(DSSDocument document, DSSDocument sha2Document) {
        super(document, sha2Document);
    }

    @Override
    protected void addErrorMessage(String errorMessage) {
        super.addErrorMessage(errorMessage);
    }

}