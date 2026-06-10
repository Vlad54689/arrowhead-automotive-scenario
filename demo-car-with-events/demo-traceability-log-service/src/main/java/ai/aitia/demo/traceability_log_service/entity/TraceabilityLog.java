package ai.aitia.demo.traceability_log_service.entity;

import java.io.Serializable;

public class TraceabilityLog implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String eventType;
	private String eventDetails;
	private long eventTimestamp;

	public TraceabilityLog() {}

	public TraceabilityLog(final int id, final int carId, final String brand, final String eventType,
			final String eventDetails, final long eventTimestamp) {
		this.id = id;
		this.carId = carId;
		this.brand = brand;
		this.eventType = eventType;
		this.eventDetails = eventDetails;
		this.eventTimestamp = eventTimestamp;
	}

	public int getId() { return id; }
	public int getCarId() { return carId; }
	public String getBrand() { return brand; }
	public String getEventType() { return eventType; }
	public String getEventDetails() { return eventDetails; }
	public long getEventTimestamp() { return eventTimestamp; }

	public void setId(final int id) { this.id = id; }
	public void setCarId(final int carId) { this.carId = carId; }
	public void setBrand(final String brand) { this.brand = brand; }
	public void setEventType(final String eventType) { this.eventType = eventType; }
	public void setEventDetails(final String eventDetails) { this.eventDetails = eventDetails; }
	public void setEventTimestamp(final long eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
