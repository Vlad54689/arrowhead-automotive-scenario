package ai.aitia.demo.risk_scoring_service.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.risk_scoring_service.publisher.event.PresetEventType;
import ai.aitia.demo.risk_scoring_service.publisher.service.PublisherService;
import ai.aitia.demo.risk_scoring_service.subscriber.constants.SubscriberConstants;
import ai.aitia.demo.risk_scoring_service.subscriber.constants.SubscriberDefaults;
import ai.aitia.demo.risk_scoring_service.util.ChainPayload;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.EventDTO;

/**
 * Middle link of the chain.
 *
 * Receives coldchain.reading.created from the Event Handler, computes a simple DETERMINISTIC risk
 * score, and re-publishes risk.assessed - propagating batchId and originTs UNCHANGED so the whole
 * chain stays correlated on one clock.
 *
 * Risk thresholds (fixed, documented):
 *   TEMP_THRESHOLD     = 8.0 C   (above this -> high risk)
 *   HUMIDITY_THRESHOLD = 85.0 %  (above this -> high risk)
 *   scorRisc = 80 (HIGH) if temperatura > 8.0 OR umiditate > 85.0, else 20 (LOW)
 */
@RestController
@RequestMapping(SubscriberDefaults.DEFAULT_EVENT_NOTIFICATION_BASE_URI)
public class RiskScoringNotificationController {

	//=================================================================================================
	// members

	// Deterministic risk thresholds (see class javadoc).
	private static final double TEMP_THRESHOLD = 8.0;
	private static final double HUMIDITY_THRESHOLD = 85.0;
	private static final int RISK_SCORE_HIGH = 80;
	private static final int RISK_SCORE_LOW = 20;

	@Autowired
	private PublisherService publisherService;

	private final Logger logger = LogManager.getLogger(RiskScoringNotificationController.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SubscriberConstants.COLDCHAIN_READING_CREATED_NOTIFICATION_URI)
	public void receiveColdChainReadingCreated(@RequestBody final EventDTO event) {
		logger.info("Received event - type: {}, payload: {}, timestamp: {}",
				event.getEventType(), event.getPayload(), event.getTimeStamp());

		final Map<String, String> in = ChainPayload.parse(event.getPayload());

		final String batchId = in.get("batchId");
		final String originTs = in.get("originTs");          // propagated UNCHANGED
		final double temperatura = parseDouble(in.get("temperatura"));
		final double umiditate = parseDouble(in.get("umiditate"));

		// Deterministic scoring.
		final int scorRisc = (temperatura > TEMP_THRESHOLD || umiditate > HUMIDITY_THRESHOLD) ? RISK_SCORE_HIGH : RISK_SCORE_LOW;

		logger.info("Computed risk - batchId={} originTs={} temperatura={} umiditate={} -> scorRisc={}",
				batchId, originTs, temperatura, umiditate, scorRisc);

		// Re-publish downstream. batchId and originTs travel UNCHANGED.
		final String payload = "batchId=" + batchId
				+ ";originTs=" + originTs
				+ ";scorRisc=" + scorRisc
				+ ";temperatura=" + temperatura
				+ ";umiditate=" + umiditate;

		publisherService.publish(PresetEventType.RISK_ASSESSED, null, payload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private double parseDouble(final String value) {
		try {
			return value == null ? 0.0 : Double.parseDouble(value);
		} catch (final NumberFormatException ex) {
			logger.warn("Could not parse numeric value '{}', defaulting to 0.0", value);
			return 0.0;
		}
	}
}
