package ai.aitia.demo.quality_inspection_service.dto;

import java.io.Serializable;

public class QualityInspectionDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int carId;
	private String brand;
	private String color;
	private boolean defectDetected;
	private String defectType;
	private String inspectionResult;
	private long inspectionTimestamp;

	public QualityInspectionDTO() {}

	public int getId() { return id; }
	public void setId(final int id) { this.id = id; }

	public int getCarId() { return carId; }
	public void setCarId(final int carId) { this.carId = carId; }

	public String getBrand() { return brand; }
	public void setBrand(final String brand) { this.brand = brand; }

	public String getColor() { return color; }
	public void setColor(final String color) { this.color = color; }

	public boolean isDefectDetected() { return defectDetected; }
	public void setDefectDetected(final boolean defectDetected) { this.defectDetected = defectDetected; }

	public String getDefectType() { return defectType; }
	public void setDefectType(final String defectType) { this.defectType = defectType; }

	public String getInspectionResult() { return inspectionResult; }
	public void setInspectionResult(final String inspectionResult) { this.inspectionResult = inspectionResult; }

	public long getInspectionTimestamp() { return inspectionTimestamp; }
	public void setInspectionTimestamp(final long inspectionTimestamp) { this.inspectionTimestamp = inspectionTimestamp; }
}
