package eu.europa.esig.dss.web.config;

import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.JdbcCacheOCSPSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.x509.aia.JdbcCacheAIASource;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLSource;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPSource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignaturePolicyProvider;
import eu.europa.esig.dss.web.validation.SkipModificationDetectionRemoteDocumentValidationService;
import eu.europa.esig.dss.ws.cert.validation.common.RemoteCertificateValidationService;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@ComponentScan(basePackages = { "eu.europa.esig.dss.web.job", "eu.europa.esig.dss.web.service" })
@Import({ PropertiesConfig.class, CXFConfig.class, JdbcConfig.class, ProxyConfiguration.class, WebSecurityConfiguration.class,
		SchedulingConfig.class })
public class DSSBeanConfig {

	private static final Logger LOG = LoggerFactory.getLogger(DSSBeanConfig.class);

	@Value("${default.validation.policy}")
	private String defaultValidationPolicy;

	@Value("${default.certificate.validation.policy}")
	private String defaultCertificateValidationPolicy;

	@Value("${current.lotl.url}")
	private String lotlUrl;

	@Value("${lotl.country.code}")
	private String lotlCountryCode;

	@Value("${current.oj.url}")
	private String currentOjUrl;

	@Value("${oj.content.keystore.type}")
	private String ksType;

	@Value("${oj.content.keystore.filename}")
	private String ksFilename;

	@Value("${oj.content.keystore.password}")
	private String ksPassword;

	@Value("${tl.loader.trust.all}")
	private boolean tlTrustAllStrategy;

	@Value("${tl.loader.cache.folder:}")
	private String tlCacheFolder;

	@Value("${dss.server.signing.keystore.type}")
	private String serverSigningKeystoreType;

	@Value("${dss.server.signing.keystore.filename}")
	private String serverSigningKeystoreFilename;

	@Value("${dss.server.signing.keystore.password}")
	private String serverSigningKeystorePassword;

	@Autowired(required = false)
	private JdbcCacheAIASource jdbcCacheAIASource;

	@Autowired(required = false)
	private JdbcCacheCRLSource jdbcCacheCRLSource;

	@Autowired(required = false)
	private JdbcCacheOCSPSource jdbcCacheOCSPSource;

	@Value("${cache.expiration:0}")
	private long cacheExpiration;

	@Value("${cache.crl.default.next.update:0}")
	private long crlDefaultNextUpdate;

	@Value("${cache.crl.max.next.update:0}")
	private long crlMaxNextUpdate;

	@Value("${cache.ocsp.default.next.update:0}")
	private long ocspDefaultNextUpdate;

	@Value("${cache.ocsp.max.next.update:0}")
	private long ocspMaxNextUpdate;

	@Value("${dataloader.connection.timeout}")
	private int connectionTimeout;
	@Value("${dataloader.connection.request.timeout}")
	private int connectionRequestTimeout;
	@Value("${dataloader.redirect.enabled}")
	private boolean redirectEnabled;
	@Value("${dataloader.use.system.properties}")
	private boolean useSystemProperties;

	// can be null
	@Autowired(required = false)
	private ProxyConfig proxyConfig;

	@Bean
	public CommonsDataLoader dataLoader() {
		return configureCommonsDataLoader(new CommonsDataLoader());
	}
	
	@Bean
	public CommonsDataLoader trustAllDataLoader() {
		CommonsDataLoader trustAllDataLoader = configureCommonsDataLoader(new CommonsDataLoader());
		trustAllDataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
		return trustAllDataLoader;
    }

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		return configureCommonsDataLoader(new OCSPDataLoader());
	}

	@Bean
	public FileCacheDataLoader fileCacheDataLoader() {
		FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
		fileCacheDataLoader.setCacheExpirationTime(cacheExpiration * 1000); // to millis
		return fileCacheDataLoader;
	}

	private FileCacheDataLoader initFileCacheDataLoader() {
		FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
		fileCacheDataLoader.setDataLoader(dataLoader());
		// Per default uses "java.io.tmpdir" property
		// fileCacheDataLoader.setFileCacheDirectory(new File("/tmp"));
		return fileCacheDataLoader;
	}

	@Bean
	public DefaultAIASource onlineAIASource() {
		return new DefaultAIASource(dataLoader());
	}

	@Bean
	public AIASource cachedAIASource() {
		if (jdbcCacheAIASource != null) {
			jdbcCacheAIASource.setProxySource(onlineAIASource());
			return jdbcCacheAIASource;
		}
		FileCacheDataLoader fileCacheDataLoader = fileCacheDataLoader();
		return new DefaultAIASource(fileCacheDataLoader);
	}

	@Bean
	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader());
		return onlineCRLSource;
	}

	@Bean
	public CRLSource cachedCRLSource() {
		if (jdbcCacheCRLSource != null) {
			jdbcCacheCRLSource.setProxySource(onlineCRLSource());
			jdbcCacheCRLSource.setDefaultNextUpdateDelay(crlDefaultNextUpdate);
			jdbcCacheCRLSource.setMaxNextUpdateDelay(crlMaxNextUpdate);
			return jdbcCacheCRLSource;
		}
		OnlineCRLSource onlineCRLSource = onlineCRLSource();
		FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
		fileCacheDataLoader.setCacheExpirationTime(crlMaxNextUpdate * 1000); // to millis
		onlineCRLSource.setDataLoader(fileCacheDataLoader);
		return onlineCRLSource;
	}

	@Bean
	public OnlineOCSPSource onlineOCSPSource() {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader());
		return onlineOCSPSource;
	}

	@Bean
	public OCSPSource cachedOCSPSource() {
		if (jdbcCacheOCSPSource != null) {
			jdbcCacheOCSPSource.setProxySource(onlineOCSPSource());
			jdbcCacheOCSPSource.setDefaultNextUpdateDelay(ocspDefaultNextUpdate);
			jdbcCacheOCSPSource.setMaxNextUpdateDelay(ocspMaxNextUpdate);
			return jdbcCacheOCSPSource;
		}
		OnlineOCSPSource onlineOCSPSource = onlineOCSPSource();
		FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
		fileCacheDataLoader.setDataLoader(ocspDataLoader());
		fileCacheDataLoader.setCacheExpirationTime(ocspMaxNextUpdate * 1000); // to millis
		onlineOCSPSource.setDataLoader(fileCacheDataLoader);
		return onlineOCSPSource;
	}

	@Bean
	public SignaturePolicyProvider signaturePolicyProvider() {
		SignaturePolicyProvider signaturePolicyProvider = new SignaturePolicyProvider();
		signaturePolicyProvider.setDataLoader(fileCacheDataLoader());
		return signaturePolicyProvider;
	}

	@Bean(name = "european-trusted-list-certificate-source")
	public TrustedListsCertificateSource trustedListSource() {
		return new TrustedListsCertificateSource();
	}

	@Bean
	public CertificateVerifier certificateVerifier() {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(cachedCRLSource());
		certificateVerifier.setOcspSource(cachedOCSPSource());
		certificateVerifier.setAIASource(cachedAIASource());
		certificateVerifier.setTrustedCertSources(trustedListSource());

		// Default configs
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setCheckRevocationForUntrustedChains(false);

		return certificateVerifier;
	}

	@Bean
	public ClassPathResource defaultPolicy() {
		return new ClassPathResource(defaultValidationPolicy);
	}

	@Bean
	public ClassPathResource defaultCertificateValidationPolicy() {
		return new ClassPathResource(defaultCertificateValidationPolicy);
	}

	@Bean
	public RemoteDocumentValidationService remoteValidationService() {
		RemoteDocumentValidationService service = new SkipModificationDetectionRemoteDocumentValidationService();
		service.setVerifier(certificateVerifier());
		if (defaultPolicy() != null) {
			try (InputStream is = defaultPolicy().getInputStream()) {
				service.setDefaultValidationPolicy(is);
			} catch (IOException e) {
				LOG.error(String.format("Unable to parse policy: %s", e.getMessage()), e);
			}
		}
		return service;
	}
	
	@Bean
	public RemoteCertificateValidationService remoteCertificateValidationService() {
		RemoteCertificateValidationService service = new RemoteCertificateValidationService();
		service.setVerifier(certificateVerifier());
		if (defaultCertificateValidationPolicy() != null) {
			try (InputStream is = defaultCertificateValidationPolicy().getInputStream()) {
				service.setDefaultValidationPolicy(is);
			} catch (IOException e) {
				LOG.error(String.format("Unable to parse policy: %s", e.getMessage()), e);
			}
		}
		return service;
	}

	@Bean
	public KeyStoreCertificateSource ojContentKeyStore() {
		try (InputStream is = new ClassPathResource(ksFilename).getInputStream()) {
			return new KeyStoreCertificateSource(is, ksType, ksPassword.toCharArray());
		} catch (IOException e) {
			throw new DSSException("Unable to load the file " + ksFilename, e);
		}
	}
	
	@Bean 
	public TLValidationJob job() {
		TLValidationJob job = new TLValidationJob();
		job.setTrustedListCertificateSource(trustedListSource());
		job.setListOfTrustedListSources(europeanLOTL());
		job.setOfflineDataLoader(offlineLoader());
		job.setOnlineDataLoader(onlineLoader());
		return job;
	}

	@Bean
	public DSSFileLoader onlineLoader() {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		onlineFileLoader.setDataLoader(tlDataLoader());
		onlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return onlineFileLoader;
	}

	@Bean
	public CommonsDataLoader tlDataLoader() {
		if (tlTrustAllStrategy) {
			LOG.info("TrustAllStrategy is enabled on TL loading.");
			return trustAllDataLoader();
		} else {
			return dataLoader();
		}
	}

	@Bean(name = "european-lotl-source")
	public LOTLSource europeanLOTL() {
		LOTLSource lotlSource = new LOTLSource();
		lotlSource.setUrl(lotlUrl);
		lotlSource.setCertificateSource(ojContentKeyStore());
		lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(currentOjUrl));
		lotlSource.setPivotSupport(true);
		return lotlSource;
	}

	@Bean
	public DSSFileLoader offlineLoader() {
		FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
		offlineFileLoader.setCacheExpirationTime(-1);
		offlineFileLoader.setDataLoader(new IgnoreDataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return offlineFileLoader;
	}

	@Bean
	public File tlCacheDirectory() {
		File tslCache;
		if (Utils.isStringEmpty(tlCacheFolder)) {
			// create temp folder
			File rootFolder = new File(System.getProperty("java.io.tmpdir"));
			tslCache = new File(rootFolder, "dss-tsl-loader");
		} else {
			tslCache = new File(tlCacheFolder);
		}
		if (tslCache.mkdirs()) {
			LOG.info("TL Cache folder : {}", tslCache.getAbsolutePath());
		}
		return tslCache;
	}

	private <C extends CommonsDataLoader> C configureCommonsDataLoader(C dataLoader) {
		dataLoader.setTimeoutConnection(connectionTimeout);
		dataLoader.setTimeoutConnectionRequest(connectionRequestTimeout);
		dataLoader.setRedirectsEnabled(redirectEnabled);
		dataLoader.setUseSystemProperties(useSystemProperties);
		dataLoader.setProxyConfig(proxyConfig);
		return dataLoader;
	}

}