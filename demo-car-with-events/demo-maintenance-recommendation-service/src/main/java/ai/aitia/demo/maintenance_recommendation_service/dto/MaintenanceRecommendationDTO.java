package ai.aitia.demo.maintenance_recommendation_service.dto;

import java.io.Serializable;

public class MaintenanceRecommendationDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String recommendationType;
	private String recommendationDetails;
	private String priority;
	private long recommendationTimestamp;

	public MaintenanceRecommendationDTO() {}

	public int getId() { return id; }
	public void setId(final int id) { this.id = id; }

	public int getCarId() { return carId; }
	public void setCarId(final int carId) { this.carId = carId; }

	public String getBrand() { return brand; }
	public void setBrand(final String brand) { this.brand = brand; }

	public String getRecommendationType() { return recommendationType; }
	public void setRecommendationType(final String recommendationType) { this.recommendationType = recommendationType; }

	public String getRecommendationDetails() { return recommendationDetails; }
	public void setRecommendationDetails(final String recommendationDetails) { this.recommendationDetails = recommendationDetails; }

	public String getPriority() { return priority; }
	public void setPriority(final String priority) { this.priority = priority; }

	public long getRecommendationTimestamp() { return recommendationTimestamp; }
	public void setRecommendationTimestamp(final long recommendationTimestamp) { this.recommendationTimestamp = recommendationTimestamp; }
}
