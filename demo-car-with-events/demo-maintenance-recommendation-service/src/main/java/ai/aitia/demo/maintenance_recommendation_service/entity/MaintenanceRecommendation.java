package ai.aitia.demo.maintenance_recommendation_service.entity;

import java.io.Serializable;

public class MaintenanceRecommendation implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String recommendationType;
	private String recommendationDetails;
	private String priority;
	private long recommendationTimestamp;

	public MaintenanceRecommendation() {}

	public MaintenanceRecommendation(final int id, final int carId, final String brand,
			final String recommendationType, final String recommendationDetails, final String priority,
			final long recommendationTimestamp) {
		this.id = id;
		this.carId = carId;
		this.brand = brand;
		this.recommendationType = recommendationType;
		this.recommendationDetails = recommendationDetails;
		this.priority = priority;
		this.recommendationTimestamp = recommendationTimestamp;
	}

	public int getId() { return id; }
	public int getCarId() { return carId; }
	public String getBrand() { return brand; }
	public String getRecommendationType() { return recommendationType; }
	public String getRecommendationDetails() { return recommendationDetails; }
	public String getPriority() { return priority; }
	public long getRecommendationTimestamp() { return recommendationTimestamp; }

	public void setId(final int id) { this.id = id; }
	public void setCarId(final int carId) { this.carId = carId; }
	public void setBrand(final String brand) { this.brand = brand; }
	public void setRecommendationType(final String recommendationType) { this.recommendationType = recommendationType; }
	public void setRecommendationDetails(final String recommendationDetails) { this.recommendationDetails = recommendationDetails; }
	public void setPriority(final String priority) { this.priority = priority; }
	public void setRecommendationTimestamp(final long recommendationTimestamp) { this.recommendationTimestamp = recommendationTimestamp; }
}
