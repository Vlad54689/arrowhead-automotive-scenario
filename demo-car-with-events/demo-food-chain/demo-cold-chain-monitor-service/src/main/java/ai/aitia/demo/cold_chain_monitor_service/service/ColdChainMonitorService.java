package ai.aitia.demo.cold_chain_monitor_service.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.demo.cold_chain_monitor_service.dto.ColdChainReadingDTO;
import ai.aitia.demo.cold_chain_monitor_service.publisher.event.PresetEventType;
import ai.aitia.demo.cold_chain_monitor_service.publisher.service.PublisherService;

/**
 * Origin link of the cold-chain event chain.
 *
 * Validates a sensor reading, stamps originTs ONCE (only if the client did not supply it), then
 * publishes coldchain.reading.created. batchId and originTs are placed into the event payload and
 * are propagated UNCHANGED through every downstream hop so the whole chain is correlated on one clock.
 */
@Service
public class ColdChainMonitorService {

	//=================================================================================================
	// members

	@Autowired
	private PublisherService publisherService;

	private final Logger logger = LogManager.getLogger(ColdChainMonitorService.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ColdChainReadingDTO ingest(final ColdChainReadingDTO dto) {
		// Validate the reading (deterministic, simple).
		if (dto.getBatchId() == null || dto.getBatchId().isBlank()) {
			throw new IllegalArgumentException("batchId is mandatory");
		}
		if (dto.getTemperatura() == null || dto.getUmiditate() == null) {
			throw new IllegalArgumentException("temperatura and umiditate are mandatory");
		}

		// originTs is set ONCE at origin: prefer the client-provided value (full client-to-verdict
		// latency on one host clock); fall back to now only if the client omitted it.
		if (dto.getOriginTs() == null) {
			dto.setOriginTs(System.currentTimeMillis());
			logger.info("originTs not supplied by client - falling back to ColdChain receipt time {}", dto.getOriginTs());
		}

		logger.info("Received reading - batchId={} originTs={} temperatura={} umiditate={} locatie={} lot={}",
				dto.getBatchId(), dto.getOriginTs(), dto.getTemperatura(), dto.getUmiditate(), dto.getLocatie(), dto.getLot());

		// Build the propagated payload. batchId and originTs MUST travel unchanged down the chain.
		final String payload = "batchId=" + dto.getBatchId()
				+ ";originTs=" + dto.getOriginTs()
				+ ";temperatura=" + dto.getTemperatura()
				+ ";umiditate=" + dto.getUmiditate()
				+ ";locatie=" + (dto.getLocatie() == null ? "" : dto.getLocatie())
				+ ";lot=" + (dto.getLot() == null ? "" : dto.getLot());

		publisherService.publish(PresetEventType.COLDCHAIN_READING_CREATED, null, payload);

		return dto;
	}
}
