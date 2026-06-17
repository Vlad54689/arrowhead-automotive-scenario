package ai.aitia.demo.risk_scoring_service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.library.ArrowheadService;
import ai.aitia.arrowhead.application.library.config.ApplicationInitListener;
import ai.aitia.arrowhead.application.library.util.ApplicationCommonConstants;
import ai.aitia.demo.risk_scoring_service.subscriber.ConfigEventProperites;
import ai.aitia.demo.risk_scoring_service.subscriber.SubscriberUtilities;
import ai.aitia.demo.risk_scoring_service.subscriber.security.SubscriberSecurityConfig;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;

/**
 * Init listener for the middle link. RiskScoring is BOTH a subscriber and a publisher, so it:
 *   1. registers its /notify endpoint (makes the system known to the SR; required before subscribing
 *      AND required to be referenceable as a provider in the 2nd hop's authorization rule),
 *   2. resolves the Event Handler ONCE (used both to subscribe to coldchain.reading.created AND to
 *      publish risk.assessed),
 *   3. subscribes to the preset upstream event.
 */
@Component
public class RiskScoringServiceApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadService arrowheadService;

	@Autowired
	private SubscriberSecurityConfig subscriberSecurityConfig;

	@Autowired
	private ConfigEventProperites configEventProperites;

	@Value(ApplicationCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
	private boolean tokenSecurityFilterEnabled;

	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;

	@Value(ApplicationCommonConstants.$APPLICATION_SYSTEM_NAME)
	private String applicationSystemName;

	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_ADDRESS_WD)
	private String applicationSystemAddress;

	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_PORT_WD)
	private int applicationSystemPort;

	private final Logger logger = LogManager.getLogger(RiskScoringServiceApplicationInitListener.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		checkConfiguration();

		//Checking the availability of necessary core systems
		checkCoreSystemReachability(CoreSystem.SERVICEREGISTRY);

		if (sslEnabled) {
			if (tokenSecurityFilterEnabled) {
				checkCoreSystemReachability(CoreSystem.AUTHORIZATION);

				//Initialize Arrowhead Context
				arrowheadService.updateCoreServiceURIs(CoreSystem.AUTHORIZATION);
				setTokenSecurityFilter();
			} else {
				logger.info("TokenSecurityFilter in not active");
			}

			setNotificationFilter();
		}

		//Register the /notify endpoint so the Event Handler can deliver here AND so the system is
		//known to the SR (prerequisite both to subscribe and to be a provider in the 2nd-hop rule).
		final ServiceRegistryRequestDTO notificationServiceRequest = createServiceRegistryRequest(RiskScoringConstants.NOTIFICATION_SERVICE_DEFINITION, RiskScoringConstants.NOTIFICATION_URI, HttpMethod.POST);
		arrowheadService.forceRegisterServiceToServiceRegistry(notificationServiceRequest);

		//Resolve the Event Handler ONCE - used both to subscribe (upstream) and to publish (downstream).
		if (arrowheadService.echoCoreSystem(CoreSystem.EVENTHANDLER)) {
			arrowheadService.updateCoreServiceURIs(CoreSystem.EVENTHANDLER);
			logger.info("Event Handler core service URIs updated - subscribe + publish enabled.");
			subscribeToPresetEvents();
		} else {
			logger.warn("Event Handler is NOT reachable - cannot subscribe upstream or publish downstream.");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void customDestroy() {
		final Map<String, String> eventTypeMap = configEventProperites.getEventTypeURIMap();
		if (eventTypeMap == null) {
			logger.info("No preset events to unsubscribe.");
		} else {
			for (final String eventType : eventTypeMap.keySet()) {
				arrowheadService.unsubscribeFromEventHandler(eventType, applicationSystemName, applicationSystemAddress, applicationSystemPort);
			}
		}

		arrowheadService.unregisterServiceFromServiceRegistry(RiskScoringConstants.NOTIFICATION_SERVICE_DEFINITION, RiskScoringConstants.NOTIFICATION_URI);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void checkConfiguration() {
		if (!sslEnabled && tokenSecurityFilterEnabled) {
			logger.warn("Contradictory configuration:");
			logger.warn("token.security.filter.enabled=true while server.ssl.enabled=false");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void setTokenSecurityFilter() {
		if (!tokenSecurityFilterEnabled || !sslEnabled) {
			logger.info("TokenSecurityFilter in not active");
		} else {
			final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
			if (authorizationPublicKey == null) {
				throw new ArrowheadException("Authorization public key is null");
			}

			KeyStore keystore;
			try {
				keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
				keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
			} catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
				throw new ArrowheadException(ex.getMessage());
			}
			final PrivateKey subscriberPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());

			final Map<String, String> eventTypeMap = configEventProperites.getEventTypeURIMap();

			subscriberSecurityConfig.getTokenSecurityFilter().setEventTypeMap( eventTypeMap );
			subscriberSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
			subscriberSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(subscriberPrivateKey);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void subscribeToPresetEvents() {
		final Map<String, String> eventTypeMap = configEventProperites.getEventTypeURIMap();

		if (eventTypeMap == null) {
			logger.info("No preset events to subscribe.");
		} else {
			final SystemRequestDTO subscriber = new SystemRequestDTO();
			subscriber.setSystemName(applicationSystemName);
			subscriber.setAddress(applicationSystemAddress);
			subscriber.setPort(applicationSystemPort);
			if (sslEnabled) {
				subscriber.setAuthenticationInfo(Base64.getEncoder().encodeToString( arrowheadService.getMyPublicKey().getEncoded()));
			}

			for (final String eventType : eventTypeMap.keySet()) {
				try {
					arrowheadService.unsubscribeFromEventHandler(eventType, applicationSystemName, applicationSystemAddress, applicationSystemPort);
				} catch (final Exception ex) {
					logger.debug("Exception happend in subscription initalization " + ex);
				}

				try {
					arrowheadService.subscribeToEventHandler(SubscriberUtilities.createSubscriptionRequestDTO(eventType, subscriber, eventTypeMap.get(eventType)));
				} catch (final InvalidParameterException ex) {
					if (ex.getMessage().contains( "Subscription violates uniqueConstraint rules")) {
						logger.debug("Subscription is already in DB");
					} else {
						logger.debug(ex.getMessage());
						logger.debug(ex);
					}
				} catch (final Exception ex) {
					logger.debug("Could not subscribe to EventType: " + eventType );
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryRequestDTO createServiceRegistryRequest(final String serviceDefinition, final String serviceUri, final HttpMethod httpMethod) {
		final ServiceRegistryRequestDTO serviceRegistryRequest = new ServiceRegistryRequestDTO();
		serviceRegistryRequest.setServiceDefinition(serviceDefinition);
		final SystemRequestDTO systemRequest = new SystemRequestDTO();
		systemRequest.setSystemName(applicationSystemName);
		systemRequest.setAddress(applicationSystemAddress);
		systemRequest.setPort(applicationSystemPort);

		if (sslEnabled && tokenSecurityFilterEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.TOKEN.name());
			serviceRegistryRequest.setInterfaces(List.of(RiskScoringConstants.INTERFACE_SECURE));
		} else if (sslEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE.name());
			serviceRegistryRequest.setInterfaces(List.of(RiskScoringConstants.INTERFACE_SECURE));
		} else {
			serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE.name());
			serviceRegistryRequest.setInterfaces(List.of(RiskScoringConstants.INTERFACE_INSECURE));
		}
		serviceRegistryRequest.setProviderSystem(systemRequest);
		serviceRegistryRequest.setServiceUri(serviceUri);
		serviceRegistryRequest.setMetadata(new HashMap<>());
		serviceRegistryRequest.getMetadata().put(RiskScoringConstants.HTTP_METHOD, httpMethod.name());
		return serviceRegistryRequest;
	}

	//-------------------------------------------------------------------------------------------------
	private void setNotificationFilter() {
		logger.debug( "setNotificationFilter started..." );

		final Map<String, String> eventTypeMap = configEventProperites.getEventTypeURIMap();

		subscriberSecurityConfig.getNotificationFilter().setEventTypeMap( eventTypeMap );
		subscriberSecurityConfig.getNotificationFilter().setServerCN( arrowheadService.getServerCN() );
	}
}
