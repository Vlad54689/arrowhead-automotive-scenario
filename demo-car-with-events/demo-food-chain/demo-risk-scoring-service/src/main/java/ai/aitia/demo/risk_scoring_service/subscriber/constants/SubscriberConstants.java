package ai.aitia.demo.risk_scoring_service.subscriber.constants;

public class SubscriberConstants {

	//=================================================================================================
	// members

	// Notification URI (under the /notify base) the Event Handler calls back on for the
	// coldchain.reading.created event. The matching value is configured in application.properties as
	// event.eventTypeURIMap[coldchain.reading.created]=coldchainreadingcreated
	public static final String COLDCHAIN_READING_CREATED_NOTIFICATION_URI = "/" + "coldchainreadingcreated";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SubscriberConstants() {
		throw new UnsupportedOperationException();
	}
}
