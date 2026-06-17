package ai.aitia.demo.supply_chain_trust_service.subscriber.constants;

public class SubscriberDefaults {

	//=================================================================================================
	// members

	public static final String DEFAULT_PRESET_EVENT_TYPES = "#{null}";
	public static final String DEFAULT_EVENT_NOTIFICATION_URI = "#{null}";
	public static final String DEFAULT_EVENT_NOTIFICATION_BASE_URI = "/notify";

	//-------------------------------------------------------------------------------------------------
	private SubscriberDefaults() {
		throw new UnsupportedOperationException();
	}
}
