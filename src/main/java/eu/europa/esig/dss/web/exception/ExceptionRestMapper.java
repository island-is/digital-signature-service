package eu.europa.esig.dss.web.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jose4j.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ExceptionRestMapper implements ExceptionMapper<Exception> {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionRestMapper.class);

	private final static Response.Status DEFAULT_ERROR_STATUS = Response.Status.INTERNAL_SERVER_ERROR;

	@Context
	UriInfo uriInfo;

	@Override
	public Response toResponse(Exception exception) {
		String errorMessage = "An error occurred during the REST request : {}";
		if (LOG.isDebugEnabled()) {
			LOG.warn(errorMessage, exception.getMessage(), exception);
		} else {
			LOG.warn(errorMessage, exception.getMessage());
		}

		String type = uriInfo != null && uriInfo.getAbsolutePath() != null ? uriInfo.getAbsolutePath().toString() : "?";
		String status = String.valueOf(DEFAULT_ERROR_STATUS.getStatusCode());
		String detail = null;
		String classInstance = exception.getStackTrace() != null ? exception.getStackTrace()[0].getClassName() : null;

		String causeTitle = null;
		String causeClassInstance = null;
		if (exception.getCause() != null) {
			causeTitle = exception.getCause().getMessage();
			causeClassInstance = exception.getCause().getStackTrace() != null ? exception.getCause().getStackTrace()[0].getClassName() : null;
		}

		if (exception instanceof JsonProcessingException) {
			JsonLocation location = ((JsonProcessingException) exception).getLocation();
			detail = location != null ? location.toString() : null;
			// clear location from main message content
			((JsonProcessingException) exception).clearLocation();
		}

		String title = exception.getMessage();

		Map<String, String> responseMap = new LinkedHashMap<>();
		if (type != null) {
			responseMap.put("type", type);
		}
		if (title != null) {
			responseMap.put("title", title);
		}
		if (status != null) {
			responseMap.put("status", status);
		}
		if (detail != null) {
			responseMap.put("detail", detail);
		}
		if (classInstance != null) {
			responseMap.put("classInstance", classInstance);
		}
		if (causeTitle != null) {
			responseMap.put("causeTitle", causeTitle);
		}
		if (causeClassInstance != null) {
			responseMap.put("causeClassInstance", causeClassInstance);
		}

		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.entity(JsonUtil.toJson(responseMap))
				.build();
	}

}
