package ai.aitia.demo.cold_chain_monitor_service.dto;

import java.io.Serializable;

/**
 * Sensor reading POSTed to the Cold Chain Monitor by the client / measurement tool.
 *
 * originTs is the single end-to-end clock reference for the whole chain. It is provided by the
 * CLIENT (the request originator) so the measured latency covers the full path from the client's
 * request to the final verdict, on one host clock. If the client omits it, ColdChain falls back to
 * its own System.currentTimeMillis() on receipt (still the same host clock).
 */
public class ColdChainReadingDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	//=================================================================================================
	// members

	private String batchId;
	private Long originTs;        // epoch millis, set ONCE at origin; may be null (then ColdChain fills it)
	private Double temperatura;   // Celsius
	private Double umiditate;     // relative humidity %
	private String locatie;
	private String lot;

	//=================================================================================================
	// methods

	public String getBatchId() { return batchId; }
	public void setBatchId(final String batchId) { this.batchId = batchId; }

	public Long getOriginTs() { return originTs; }
	public void setOriginTs(final Long originTs) { this.originTs = originTs; }

	public Double getTemperatura() { return temperatura; }
	public void setTemperatura(final Double temperatura) { this.temperatura = temperatura; }

	public Double getUmiditate() { return umiditate; }
	public void setUmiditate(final Double umiditate) { this.umiditate = umiditate; }

	public String getLocatie() { return locatie; }
	public void setLocatie(final String locatie) { this.locatie = locatie; }

	public String getLot() { return lot; }
	public void setLot(final String lot) { this.lot = lot; }
}
