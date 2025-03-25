package eu.europa.esig.dss.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import eu.europa.esig.dss.web.exception.ExceptionRestMapper;
import eu.europa.esig.dss.ws.cert.validation.common.RemoteCertificateValidationService;
import eu.europa.esig.dss.ws.cert.validation.rest.RestCertificateValidationServiceImpl;
import eu.europa.esig.dss.ws.cert.validation.rest.client.RestCertificateValidationService;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.ws.validation.rest.RestDocumentValidationServiceImpl;
import eu.europa.esig.dss.ws.validation.rest.client.RestDocumentValidationService;
import jakarta.annotation.PostConstruct;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.util.Collections;
import java.util.List;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class CXFConfig {

	public static final String REST_VALIDATION = "/rest/validation";
	public static final String REST_CERTIFICATE_VALIDATION = "/rest/certificate-validation";

	@Value("${cxf.debug:false}")
	private boolean cxfDebug;

	@Value("${cxf.mtom.enabled:true}")
	private boolean mtomEnabled;

	@Value("${dssVersion:1.0}")
	private String dssVersion;

	@Autowired
	private Bus bus;

	@Autowired
	private RemoteDocumentValidationService remoteValidationService;

	@Autowired
	private RemoteCertificateValidationService remoteCertificateValidationService;

	@Bean
	public ServletRegistrationBean<CXFServlet> cxfServlet() {
		final ServletRegistrationBean<CXFServlet> servletRegistrationBean =
				new ServletRegistrationBean<>(new CXFServlet(), "/services/*");
		servletRegistrationBean.setLoadOnStartup(1); // priority order on load
		return servletRegistrationBean;
	}

	@PostConstruct
	private void addLoggers() {
		if (cxfDebug) {
			LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
			bus.getInInterceptors().add(loggingInInterceptor);
			bus.getInFaultInterceptors().add(loggingInInterceptor);

			LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
			bus.getOutInterceptors().add(loggingOutInterceptor);
			bus.getOutFaultInterceptors().add(loggingOutInterceptor);
		}
	}

	// --------------- REST

	@Bean
	public RestDocumentValidationService restValidationService() {
		RestDocumentValidationServiceImpl service = new RestDocumentValidationServiceImpl();
		service.setValidationService(remoteValidationService);
		return service;
	}

	@Bean
	public RestCertificateValidationService restCertificateValidationService() {
		RestCertificateValidationServiceImpl service = new RestCertificateValidationServiceImpl();
		service.setValidationService(remoteCertificateValidationService);
		return service;
	}

	@Bean
	public Server createServerValidationRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setServiceBean(restValidationService());
		sfb.setAddress(REST_VALIDATION);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
        sfb.setFeatures(createFeatures(RestDocumentValidationService.class.getName()));
		return sfb.create();
	}

	@Bean
	public Server createServerCertificateValidationRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setServiceBean(restCertificateValidationService());
		sfb.setAddress(REST_CERTIFICATE_VALIDATION);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
        sfb.setFeatures(createFeatures(RestCertificateValidationService.class.getName()));
		return sfb.create();
	}

    private List<OpenApiFeature> createFeatures(String resourcesClassName) {
        return Collections.singletonList(createOpenApiFeature(resourcesClassName));
    }

    private OpenApiFeature createOpenApiFeature(String resourcesClassName) {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
		openApiFeature.setCustomizer(openApiCustomizer());
        openApiFeature.setPrettyPrint(true);
        openApiFeature.setScan(true);
		openApiFeature.setUseContextBasedConfig(true);
        openApiFeature.setTitle("DSS WebServices");
		openApiFeature.setVersion(dssVersion);
        openApiFeature.setResourceClasses(Collections.singleton(resourcesClassName));
        return openApiFeature;
    }

    private OpenApiCustomizer openApiCustomizer() {
		OpenApiCustomizer customizer = new OpenApiCustomizer();
		customizer.setDynamicBasePath(true);
		return customizer;
	}

	@Bean
	public JacksonJsonProvider jacksonJsonProvider() {
		JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
		jsonProvider.setMapper(objectMapper());
		return jsonProvider;
	}
    
	/**
	 * ObjectMappers configures a proper way for (un)marshalling of json data
	 *
	 * @return {@link ObjectMapper}
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// true value allows processing of {@code @IDREF}s cycles
		JakartaXmlBindAnnotationIntrospector jai = new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance());
		objectMapper.setAnnotationIntrospector(jai);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(DeserializationFeature.WRAP_EXCEPTIONS, false);
		return objectMapper;
	}
	
	@Bean
	public ExceptionRestMapper exceptionRestMapper() {
		return new ExceptionRestMapper();
	}

}
