package ai.aitia.demo.risk_scoring_service.publisher.event;

import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

public enum PresetEventType {

	//=================================================================================================
	// elements

	RISK_ASSESSED( EventTypeConstants.EVENT_TYPE_RISK_ASSESSED, List.of() );

	//=================================================================================================
	// members

	private final String eventTypeName;
	private final List<String> metadataKeys;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String getEventTypeName() { return eventTypeName; }
	public List<String> getMetadataKeys() { return metadataKeys; }

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private PresetEventType( final String eventTypeName, final List<String> metadataKeys ) {
		Assert.isTrue(!Utilities.isEmpty(eventTypeName), "EventType name is invalid.");

		this.metadataKeys = metadataKeys != null ? Collections.unmodifiableList(metadataKeys) : List.of();
		this.eventTypeName = eventTypeName;
	}
}
