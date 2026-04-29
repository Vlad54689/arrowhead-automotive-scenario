package ai.aitia.demo.traceability_log_service.dto;

import java.io.Serializable;

public class TraceabilityLogDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String eventType;
	private String eventDetails;
	private long eventTimestamp;

	public TraceabilityLogDTO() {}

	public int getId() { return id; }
	public void setId(final int id) { this.id = id; }

	public int getCarId() { return carId; }
	public void setCarId(final int carId) { this.carId = carId; }

	public String getBrand() { return brand; }
	public void setBrand(final String brand) { this.brand = brand; }

	public String getEventType() { return eventType; }
	public void setEventType(final String eventType) { this.eventType = eventType; }

	public String getEventDetails() { return eventDetails; }
	public void setEventDetails(final String eventDetails) { this.eventDetails = eventDetails; }

	public long getEventTimestamp() { return eventTimestamp; }
	public void setEventTimestamp(final long eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
