package ai.aitia.demo.supply_chain_trust_service.subscriber.constants;

public class SubscriberConstants {

	//=================================================================================================
	// members

	// Notification URI (under the /notify base) the Event Handler calls back on for the risk.assessed
	// event. The matching value is configured in application.properties as
	// event.eventTypeURIMap[risk.assessed]=riskassessed
	public static final String RISK_ASSESSED_NOTIFICATION_URI = "/" + "riskassessed";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SubscriberConstants() {
		throw new UnsupportedOperationException();
	}
}
