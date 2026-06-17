package ai.aitia.demo.cold_chain_monitor_service;

public class ColdChainMonitorConstants {

	//=================================================================================================
	// members

	public static final String BASE_PACKAGE = "ai.aitia";

	// Arrowhead system name (lowercase, no dots/underscores - URI-safe).
	public static final String SYSTEM_NAME = "coldchainmonitor";

	// Business service this publisher registers in the Service Registry. ColdChain is the origin
	// of the chain: it accepts a sensor reading over REST and publishes coldchain.reading.created.
	// Registering this service also makes the system known to the Service Registry, which is the
	// prerequisite for it to be referenced as a provider in a per-hop intra-cloud authorization rule.
	public static final String READING_SERVICE_DEFINITION = "coldchain-reading";
	public static final String READING_URI = "/cold-chain/readings";

	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ColdChainMonitorConstants() {
		throw new UnsupportedOperationException();
	}
}
