package ai.aitia.demo.supply_chain_trust_service;

public class SupplyChainTrustConstants {

	//=================================================================================================
	// members

	public static final String BASE_PACKAGE = "ai.aitia";

	// Arrowhead system name (lowercase, URI-safe).
	public static final String SYSTEM_NAME = "supplychaintrust";

	// Final, subscribe-only link. Registers its /notify endpoint so it is known to the Service
	// Registry (prerequisite for the Event Handler to accept its subscription to risk.assessed).
	public static final String NOTIFICATION_SERVICE_DEFINITION = "supply-chain-trust-notify";
	public static final String NOTIFICATION_URI = "/notify";

	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SupplyChainTrustConstants() {
		throw new UnsupportedOperationException();
	}
}
