package ai.aitia.demo.supply_chain_trust_service.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.supply_chain_trust_service.subscriber.constants.SubscriberConstants;
import ai.aitia.demo.supply_chain_trust_service.subscriber.constants.SubscriberDefaults;
import ai.aitia.demo.supply_chain_trust_service.util.ChainPayload;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.EventDTO;

/**
 * Final link of the chain.
 *
 * Receives risk.assessed, applies a simple DETERMINISTIC trust rule, and LOGS the verdict together
 * with batchId, originTs and the cumulative end-to-end latency (now - originTs), measured on the
 * single host clock.
 *
 * Trust rule (fixed, documented):
 *   TRUST_SCORE_THRESHOLD = 50
 *   verdict = DE_INCREDERE if scorRisc < 50, else SUSPECT
 *
 * The verdict line has a fixed, easy-to-parse shape for the measurement tooling:
 *   TRUST verdict batchId=<id> originTs=<ts> latency_ms=<val> verdict=<DE_INCREDERE|SUSPECT>
 */
@RestController
@RequestMapping(SubscriberDefaults.DEFAULT_EVENT_NOTIFICATION_BASE_URI)
public class SupplyChainTrustNotificationController {

	//=================================================================================================
	// members

	// Deterministic trust threshold (see class javadoc).
	private static final int TRUST_SCORE_THRESHOLD = 50;
	private static final String VERDICT_TRUSTED = "DE_INCREDERE";
	private static final String VERDICT_SUSPECT = "SUSPECT";

	private final Logger logger = LogManager.getLogger(SupplyChainTrustNotificationController.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SubscriberConstants.RISK_ASSESSED_NOTIFICATION_URI)
	public void receiveRiskAssessed(@RequestBody final EventDTO event) {
		logger.info("Received event - type: {}, payload: {}, timestamp: {}",
				event.getEventType(), event.getPayload(), event.getTimeStamp());

		final Map<String, String> in = ChainPayload.parse(event.getPayload());

		final String batchId = in.get("batchId");
		final String originTsRaw = in.get("originTs");   // propagated UNCHANGED from origin
		final int scorRisc = parseInt(in.get("scorRisc"));

		final String verdict = scorRisc < TRUST_SCORE_THRESHOLD ? VERDICT_TRUSTED : VERDICT_SUSPECT;

		// Cumulative end-to-end latency: now (host clock) - originTs (set once at origin, same clock).
		final Long originTs = parseLongOrNull(originTsRaw);
		final long latencyMs = originTs == null ? -1L : (System.currentTimeMillis() - originTs);

		// Fixed, parser-friendly verdict line.
		logger.info("TRUST verdict batchId={} originTs={} latency_ms={} verdict={}",
				batchId, originTsRaw, latencyMs, verdict);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private int parseInt(final String value) {
		try {
			return value == null ? Integer.MAX_VALUE : Integer.parseInt(value.trim());
		} catch (final NumberFormatException ex) {
			logger.warn("Could not parse scorRisc '{}', treating as SUSPECT", value);
			return Integer.MAX_VALUE;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Long parseLongOrNull(final String value) {
		try {
			return value == null ? null : Long.parseLong(value.trim());
		} catch (final NumberFormatException ex) {
			logger.warn("Could not parse originTs '{}', latency cannot be computed", value);
			return null;
		}
	}
}
