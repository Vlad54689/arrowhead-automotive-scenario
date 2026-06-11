package ai.aitia.demo.maintenance_recommendation_service.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.maintenance_recommendation_service.subscriber.constants.SubscriberConstants;
import ai.aitia.demo.maintenance_recommendation_service.subscriber.constants.SubscriberDefaults;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.EventDTO;

@RestController
@RequestMapping(SubscriberDefaults.DEFAULT_EVENT_NOTIFICATION_BASE_URI)
public class MaintenanceRecommendationNotificationController {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(MaintenanceRecommendationNotificationController.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SubscriberConstants.QUALITY_INSPECTION_CREATED_NOTIFICATION_URI)
	public void receiveQualityInspectionCreated(@RequestBody final EventDTO event) {
		logger.info("Received event - type: {}, payload: {}, timestamp: {}",
				event.getEventType(), event.getPayload(), event.getTimeStamp());
	}
}
