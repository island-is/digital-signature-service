package eu.europa.esig.dss.web.job.sha2;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.spi.client.http.DSSCacheFileLoader;
import eu.europa.esig.dss.tsl.sha2.Sha2FileCacheDataLoader;
import eu.europa.esig.dss.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Sha2FileCacheDataLoaderDebug extends Sha2FileCacheDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(Sha2FileCacheDataLoaderDebug.class);

    /** Defines a one-day constraint in milliseconds */
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    /**
     * Creates an object with a defined {@code DSSCacheFileLoader}.
     * The {@code predicate} shall be provided with a setter.
     *
     * @param dataLoader {@link DSSCacheFileLoader} to use
     */
    public Sha2FileCacheDataLoaderDebug(DSSCacheFileLoader dataLoader) {
        super(dataLoader);
    }

    /**
     * This method instantiates a Sha2FileCacheDataLoader with a pre-configured predicate, forcing a Trusted List
     * refresh in case of an updated .sha2 document, when a NextUpdate has been reached,
     * or when the document has not been re-downloaded for at least a day.
     *
     * @param dataLoader {@link DSSCacheFileLoader} to be used to load the document
     * @return {@link Sha2FileCacheDataLoader}
     */
    public static Sha2FileCacheDataLoaderDebug initSha2DailyUpdateDataLoader(DSSCacheFileLoader dataLoader) {
        Sha2FileCacheDataLoaderDebug sha2DataLoader = new Sha2FileCacheDataLoaderDebug(dataLoader);

        DefaultTrustedListWithSha2PredicateDebug sha2Predicate = new DefaultTrustedListWithSha2PredicateDebug();
        sha2Predicate.setCacheExpirationTime(ONE_DAY_MILLIS);
        sha2DataLoader.setPredicate(sha2Predicate);

        return sha2DataLoader;
    }

    @Override
    public DSSDocument getDocument(String url, boolean refresh) {
        Objects.requireNonNull(url, "URL cannot be null!");

        assertConfigurationIsValid();

        DSSDocument sha2Document = null;
        String sha2ExtractionStatus = null;
        try {
            sha2Document = getSha2File(url);
        } catch (Exception e) {
            sha2ExtractionStatus = e.getMessage();
            String errorMessage = String.format("No sha2 document has been found : %s", sha2ExtractionStatus);
            if (LOG.isDebugEnabled()) {
                LOG.debug(errorMessage, e);
            }
            refresh = true; // force the refresh
        }

        DSSDocument cachedDocument = null;
        DSSDocument refreshedDocument = null;
        if (refresh) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Refresh has been requested for a document with URL '{}'", url);
            }
            refreshedDocument = getRefreshedDocument(url);

        } else {
            cachedDocument = getDocumentFromCache(url);
            if (cachedDocument == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No cached document found for URL '{}'", url);
                }
                refreshedDocument = getRefreshedDocument(url);
            }
        }

        DocumentWithSha2Debug documentWithSha2 = null;
        if (cachedDocument != null) {
            documentWithSha2 = mergeDocumentWithSha2(cachedDocument, sha2Document);
            if (checkRefreshRequired(documentWithSha2)) {
                LOG.debug("Refresh the document from URL '{}'...", url);
                refreshedDocument = getRefreshedDocument(url);

            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sha2 document condition match. Return cached document for URL '{}'", url);
                }
            }
        }

        if (refreshedDocument != null) {
            documentWithSha2 = mergeDocumentWithSha2(refreshedDocument, sha2Document);
            checkRefreshRequired(documentWithSha2); // verify if new document matches sha2
        }

        if (documentWithSha2 != null && Utils.isStringNotEmpty(sha2ExtractionStatus)) {
            documentWithSha2.addErrorMessage(sha2ExtractionStatus);
        }

        return documentWithSha2;
    }

    @Override
    protected DocumentWithSha2Debug mergeDocumentWithSha2(DSSDocument cachedDocument, DSSDocument sha2Document) {
        return new DocumentWithSha2Debug(cachedDocument, sha2Document);
    }

}
