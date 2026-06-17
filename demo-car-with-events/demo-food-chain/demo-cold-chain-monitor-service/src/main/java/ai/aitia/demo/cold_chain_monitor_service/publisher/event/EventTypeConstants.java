package ai.aitia.demo.cold_chain_monitor_service.publisher.event;

public class EventTypeConstants {

	//=================================================================================================
	// members

	// First link of the chain. RiskScoring subscribes to this event type.
	public static final String EVENT_TYPE_COLDCHAIN_READING_CREATED = "coldchain.reading.created";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private EventTypeConstants() {
		throw new UnsupportedOperationException();
	}
}
