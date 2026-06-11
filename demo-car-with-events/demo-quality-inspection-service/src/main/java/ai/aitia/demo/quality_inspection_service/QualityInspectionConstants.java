package ai.aitia.demo.quality_inspection_service;

public class QualityInspectionConstants {

	//=================================================================================================
	// members

	public static final String BASE_PACKAGE = "ai.aitia";

	public static final String SERVICE_NAME = "quality-inspection-service";
	public static final String SERVICE_TYPE = "quality-inspection";

	// Arrowhead service registration (system name `qualityinspection`)
	public static final String INSPECTION_SERVICE_DEFINITION = "inspection";
	public static final String INSPECTION_URI = "/inspection";

	public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
	public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
	public static final String HTTP_METHOD = "http-method";
	public static final String CAR_URI = "/quality-inspections";
	public static final String BY_ID_PATH = "/{id}";
	public static final String PATH_VARIABLE_ID = "id";
	public static final String REQUEST_PARAM_CAR_ID = "car-id";
	public static final String REQUEST_PARAM_BRAND = "brand";
	public static final String REQUEST_PARAM_COLOR = "color";
	public static final String REQUEST_PARAM_INSPECT = "inspect";

	public static final String SERVICE_LIMIT = "service_limit";
	public static final int DEFAULT_SERVICE_LIMIT = 200;
	public static final String $SERVICE_LIMIT_WD = "${" + SERVICE_LIMIT + ":" + DEFAULT_SERVICE_LIMIT + "}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private QualityInspectionConstants() {
		throw new UnsupportedOperationException();
	}
}
