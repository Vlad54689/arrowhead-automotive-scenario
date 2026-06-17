package ai.aitia.demo.risk_scoring_service;

public class RiskScoringConstants {

	//=================================================================================================
	// members

	public static final String BASE_PACKAGE = "ai.aitia";

	// Arrowhead system name (lowercase, URI-safe).
	public static final String SYSTEM_NAME = "riskscoring";

	// This system is BOTH a subscriber and a publisher. As a subscriber it must be known to the
	// Service Registry before the Event Handler accepts a subscription, so it registers its /notify
	// endpoint. That registration also makes riskscoring referenceable as a PROVIDER in the second
	// hop's authorization rule (consumer=supplychaintrust, provider=riskscoring).
	public static final String NOTIFICATION_SERVICE_DEFINITION = "risk-scoring-notify";
	public static final String NOTIFICATION_URI = "/notify";

	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private RiskScoringConstants() {
		throw new UnsupportedOperationException();
	}
}
