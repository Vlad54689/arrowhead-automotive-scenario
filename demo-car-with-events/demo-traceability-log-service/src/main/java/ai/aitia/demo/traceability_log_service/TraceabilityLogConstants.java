package ai.aitia.demo.traceability_log_service;

public class TraceabilityLogConstants {

	//=================================================================================================
	// members

	public static final String BASE_PACKAGE = "ai.aitia";

	public static final String SERVICE_NAME = "traceability-log-service";
	public static final String SERVICE_TYPE = "traceability-log";
	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";
	public static final String CAR_URI = "/traceability-logs";
	public static final String BY_ID_PATH = "/{id}";
	public static final String PATH_VARIABLE_ID = "id";
	public static final String REQUEST_PARAM_CAR_ID = "car-id";
	public static final String REQUEST_PARAM_BRAND = "brand";
	public static final String REQUEST_PARAM_EVENT_TYPE = "event-type";

	public static final String SERVICE_LIMIT = "service_limit";
	public static final int DEFAULT_SERVICE_LIMIT = 200;
	public static final String $SERVICE_LIMIT_WD = "${" + SERVICE_LIMIT + ":" + DEFAULT_SERVICE_LIMIT + "}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TraceabilityLogConstants() {
		throw new UnsupportedOperationException();
	}
}
