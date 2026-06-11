package ai.aitia.demo.maintenance_recommendation_service.subscriber.constants;

public class SubscriberConstants {

	//=================================================================================================
	// members

	// Notification URI (under the /notify base) that the Event Handler calls back on for the
	// quality.inspection.created event. The matching value is configured in application.properties as
	// event.eventTypeURIMap[quality.inspection.created]=qualityinspectioncreated
	public static final String QUALITY_INSPECTION_CREATED_NOTIFICATION_URI = "/" + "qualityinspectioncreated";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SubscriberConstants() {
		throw new UnsupportedOperationException();
	}
}
