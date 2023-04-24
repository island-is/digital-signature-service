package eu.europa.esig.dss.web.service;

import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.pdf.IPdfObjFactory;
import eu.europa.esig.dss.pdf.ServiceLoaderPdfObjFactory;
import eu.europa.esig.dss.pdf.modifications.DefaultPdfDifferencesFinder;
import eu.europa.esig.dss.validation.SignedDocumentValidator;

public class ValidationService {

    public static SignedDocumentValidator configureDocumentValidator(SignedDocumentValidator documentValidator) {
        // Skip visual document comparison
        if (documentValidator instanceof PDFDocumentValidator) {
            PDFDocumentValidator pdfDocumentValidator = (PDFDocumentValidator) documentValidator;

            IPdfObjFactory pdfObjFactory = new ServiceLoaderPdfObjFactory();

            DefaultPdfDifferencesFinder pdfDifferencesFinder = new DefaultPdfDifferencesFinder();
            pdfDifferencesFinder.setMaximalPagesAmountForVisualComparison(0);
            pdfObjFactory.setPdfDifferencesFinder(pdfDifferencesFinder);

            pdfDocumentValidator.setPdfObjFactory(pdfObjFactory);
        }
        return documentValidator;
    }

}
