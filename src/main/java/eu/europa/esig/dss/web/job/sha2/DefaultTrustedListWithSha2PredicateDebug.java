package eu.europa.esig.dss.web.job.sha2;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.tsl.sha2.DefaultTrustedListWithSha2Predicate;
import eu.europa.esig.dss.tsl.sha2.DocumentWithSha2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

public class DefaultTrustedListWithSha2PredicateDebug extends DefaultTrustedListWithSha2Predicate {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTrustedListWithSha2Predicate.class);

    /**
     * Default constructor
     */
    public DefaultTrustedListWithSha2PredicateDebug() {
        // empty
    }

    @Override
    public boolean test(DocumentWithSha2 documentWithSha2) {
        Objects.requireNonNull(documentWithSha2, "Document shall be provided!");

        DocumentWithSha2Debug documentWithSha2Debug = (DocumentWithSha2Debug) documentWithSha2;

        /*
         * For example, the TLSOx's TL published at the location http://www.TLSOx.xyz/TrustedList/TL.xml
         * is accompanied by its sha2 digest file i.e. on location http://www.TLSOx.xyz/TrustedList/TL.sha2.
         * Downloaders may adopt the following strategy for downloading file TL.xml:
         */
        DSSDocument document = documentWithSha2Debug.getDocument();
        if (document == null) {
            String error = "No cached document has been found.";
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}", error);
            }
            documentWithSha2Debug.addErrorMessage(error);
            return false; // refresh required
        }
        /*
         * - check whether TL.sha2 is available for do [11] wnload:
         *     - if TL.sha2 has been successfully downloaded, verify the digest against the cached TL.xml file. If
         *       different, download and process TL.xml;
         *     - if TL.sha2 has not been successfully downloaded, download and process TL.xml directly.
         */
        DSSDocument sha2Document = documentWithSha2Debug.getSha2Document();
        if (sha2Document == null) {
            String error = "No sha2 document has been found.";
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}", error);
            }
            documentWithSha2Debug.addErrorMessage(error);
            return false;

        } else {
            Digest originalDocumentDigest = getOriginalDocumentDigest(document);
            Digest sha2Digest = getSha2Digest(sha2Document);
            if (!originalDocumentDigest.equals(sha2Digest)) {
                String error = String.format("Digest present within sha2 file '%s' do not match digest of " +
                                "the cached document '%s'.",
                        new String(DSSUtils.toByteArray(sha2Document)), originalDocumentDigest.getHexValue().toLowerCase());
                LOG.debug(String.format("%s Document name: '%s'", error, document.getName()));
                documentWithSha2Debug.addErrorMessage(error);
                return false;
            }
        }
        /*
         * - TL.xml should be downloaded/processed anyway if the nextUpdate (in the cached file) has been reached.
         */
        Date nextUpdate = getNextUpdate(document);
        if (nextUpdate != null && !nextUpdate.after(getCurrentTime())) {
            String error = String.format("NextUpdate '%s' has been reached.", DSSUtils.formatDateToRFC(nextUpdate));
            if (LOG.isDebugEnabled()) {
                LOG.debug(error);
            }
            documentWithSha2Debug.addErrorMessage(error);
            return false;
        }
        // Optional : validate cache expiration
        if (isCacheExpired(document)) {
            LOG.info("Cache of the document with name '{}' expired. Request refresh.", document.getName());
            return false;
        }
        // accept the document otherwise
        return true;
    }

}
