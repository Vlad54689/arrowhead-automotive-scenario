package ai.aitia.demo.risk_scoring_service.publisher.event;

public class EventTypeConstants {

	//=================================================================================================
	// members

	// Second link of the chain. SupplyChainTrust subscribes to this event type.
	public static final String EVENT_TYPE_RISK_ASSESSED = "risk.assessed";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private EventTypeConstants() {
		throw new UnsupportedOperationException();
	}
}
