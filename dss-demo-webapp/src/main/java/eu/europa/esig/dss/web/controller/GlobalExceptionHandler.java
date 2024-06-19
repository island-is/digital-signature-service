package eu.europa.esig.dss.web.controller;

import eu.europa.esig.dss.exception.IllegalInputException;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.web.exception.SignatureOperationException;
import eu.europa.esig.dss.web.exception.InternalServerException;
import eu.europa.esig.dss.web.exception.SourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public static final String DEFAULT_ERROR_VIEW = "error";
	public static final String PAGE_NOT_FOUND_ERROR_VIEW = "404_error";
	
    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
		// If the exception is annotated with @ResponseStatus rethrow it and let
		// the framework handle it (for personal and annotated Exception)
		if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
			throw e;
		}

		LOG.error("Unhandled exception occurred : " + e.getMessage(), e);
		
        return getMAV(req, e, HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_ERROR_VIEW);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ModelAndView pageNotFoundErrorHandler(HttpServletRequest req, Exception e) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("The page [{}] does not exist : {}", req.getRequestURI(), e.getMessage());
		}
		return getMAV(req, e, HttpStatus.NOT_FOUND, PAGE_NOT_FOUND_ERROR_VIEW);
	}

	@ExceptionHandler(SourceNotFoundException.class)
	public ModelAndView sourceNotFoundErrorHandler(HttpServletRequest req, Exception e) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("The page [{}] does not exist : {}", req.getRequestURI(), e.getMessage());
		}
		return getMAV(req, e, HttpStatus.NOT_FOUND, PAGE_NOT_FOUND_ERROR_VIEW);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ModelAndView wrongArgument(HttpServletRequest req, Exception e) {
		return getMAV(req, new DSSException("Bad Request"), HttpStatus.BAD_REQUEST, DEFAULT_ERROR_VIEW);
	}
    
    @ExceptionHandler(InternalServerException.class)
    public ModelAndView internalServer(HttpServletRequest req, Exception e) {
        return getMAV(req, e, HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_ERROR_VIEW);
    }

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ModelAndView maxUploadSizeExceededExceptionHandler(HttpServletRequest req, Exception e) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("The max upload file size exceeded. Reason : {}", e.getMessage());
		}
		return getMAV(req, new DSSException("Uploaded file size exceeded max allowed limit."), HttpStatus.FORBIDDEN, DEFAULT_ERROR_VIEW);
	}

	@ExceptionHandler({UnsupportedOperationException.class, IllegalArgumentException.class, IllegalInputException.class})
	public ModelAndView unsupportedOperationExceptionHandler(HttpServletRequest req, Exception e) {
		String errorMessage = "An error occurred on URI call [{}] : {}";
		if (LOG.isDebugEnabled()) {
			LOG.warn(errorMessage, req.getRequestURI(), e.getMessage(), e);
		} else {
			LOG.warn(errorMessage, req.getRequestURI(), e.getMessage());
		}
		return getMAV(req, e, HttpStatus.BAD_REQUEST, DEFAULT_ERROR_VIEW);
	}

	private ModelAndView getMAV(HttpServletRequest req, Exception e, HttpStatus status, String viewName) {
		ModelAndView mav = new ModelAndView();
		mav.addObject("error", status.getReasonPhrase());
		mav.addObject("exception", e.getMessage());
		mav.addObject("url", req.getRequestURL());
		mav.setStatus(status);
		mav.setViewName(viewName);
		return mav;
	}

	@ExceptionHandler(SignatureOperationException.class)
	public ResponseEntity<String> signatureOperationExceptionHandler(HttpServletRequest req, Exception e) throws Exception {
		LOG.error("An error occurred on URI call [{}] : {}", req.getRequestURI(), e.getMessage(), e);
		return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

}