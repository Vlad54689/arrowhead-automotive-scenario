package ai.aitia.demo.quality_inspection_service.entity;

import java.io.Serializable;

public class QualityInspection implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String color;
	private boolean isDefectDetected;
	private String defectType;
	private String inspectionResult;
	private long inspectionTimestamp;

	public QualityInspection() {}

	public QualityInspection(final int id, final int carId, final String brand, final String color,
			final boolean isDefectDetected, final String defectType, final String inspectionResult,
			final long inspectionTimestamp) {
		this.id = id;
		this.carId = carId;
		this.brand = brand;
		this.color = color;
		this.isDefectDetected = isDefectDetected;
		this.defectType = defectType;
		this.inspectionResult = inspectionResult;
		this.inspectionTimestamp = inspectionTimestamp;
	}

	public int getId() { return id; }
	public int getCarId() { return carId; }
	public String getBrand() { return brand; }
	public String getColor() { return color; }
	public boolean isDefectDetected() { return isDefectDetected; }
	public String getDefectType() { return defectType; }
	public String getInspectionResult() { return inspectionResult; }
	public long getInspectionTimestamp() { return inspectionTimestamp; }

	public void setId(final int id) { this.id = id; }
	public void setCarId(final int carId) { this.carId = carId; }
	public void setBrand(final String brand) { this.brand = brand; }
	public void setColor(final String color) { this.color = color; }
	public void setDefectDetected(final boolean isDefectDetected) { this.isDefectDetected = isDefectDetected; }
	public void setDefectType(final String defectType) { this.defectType = defectType; }
	public void setInspectionResult(final String inspectionResult) { this.inspectionResult = inspectionResult; }
	public void setInspectionTimestamp(final long inspectionTimestamp) { this.inspectionTimestamp = inspectionTimestamp; }
}
