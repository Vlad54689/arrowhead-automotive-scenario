package ai.aitia.demo.quality_inspection_service.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.demo.quality_inspection_service.database.InMemoryQualityInspectionDB;
import ai.aitia.demo.quality_inspection_service.dto.QualityInspectionDTO;
import ai.aitia.demo.quality_inspection_service.entity.QualityInspection;
import ai.aitia.demo.quality_inspection_service.publisher.event.PresetEventType;
import ai.aitia.demo.quality_inspection_service.publisher.service.PublisherService;

@Service
public class QualityInspectionService {

	@Autowired
	private InMemoryQualityInspectionDB database;

	@Autowired
	private PublisherService publisherService;

	private final Random random = new Random();

	public QualityInspectionDTO create(final QualityInspectionDTO dto) {
		final QualityInspection inspection = new QualityInspection();
		inspection.setCarId(dto.getCarId());
		inspection.setBrand(dto.getBrand());
		inspection.setColor(dto.getColor());
		inspection.setDefectDetected(dto.isDefectDetected());
		inspection.setDefectType(dto.getDefectType());
		inspection.setInspectionResult(dto.getInspectionResult());
		inspection.setInspectionTimestamp(dto.getInspectionTimestamp());
		final int id = database.create(inspection);
		inspection.setId(id);

		//Publish a quality.inspection.created event after the inspection is persisted
		final String payload = "inspectionId=" + inspection.getId()
				+ ";carId=" + inspection.getCarId()
				+ ";inspectionResult=" + inspection.getInspectionResult()
				+ ";timestamp=" + inspection.getInspectionTimestamp();
		publisherService.publish(PresetEventType.QUALITY_INSPECTION_CREATED, null, payload);

		return convertToDTO(inspection);
	}

	public QualityInspectionDTO getById(final int id) {
		final QualityInspection inspection = database.getById(id);
		return inspection != null ? convertToDTO(inspection) : null;
	}

	public List<QualityInspectionDTO> getAll() {
		final List<QualityInspection> inspections = database.getAll();
		final List<QualityInspectionDTO> dtos = new ArrayList<>();
		for (final QualityInspection inspection : inspections) {
			dtos.add(convertToDTO(inspection));
		}
		return dtos;
	}

	public List<QualityInspectionDTO> getByCarId(final int carId) {
		final List<QualityInspection> inspections = database.getByCarId(carId);
		final List<QualityInspectionDTO> dtos = new ArrayList<>();
		for (final QualityInspection inspection : inspections) {
			dtos.add(convertToDTO(inspection));
		}
		return dtos;
	}

	public List<QualityInspectionDTO> search(final Map<String, String> params) {
		final List<QualityInspection> inspections = database.search(params);
		final List<QualityInspectionDTO> dtos = new ArrayList<>();
		for (final QualityInspection inspection : inspections) {
			dtos.add(convertToDTO(inspection));
		}
		return dtos;
	}

	public QualityInspectionDTO update(final int id, final QualityInspectionDTO dto) {
		final QualityInspection existing = database.getById(id);
		if (existing == null) {
			return null;
		}
		existing.setCarId(dto.getCarId());
		existing.setBrand(dto.getBrand());
		existing.setColor(dto.getColor());
		existing.setDefectDetected(dto.isDefectDetected());
		existing.setDefectType(dto.getDefectType());
		existing.setInspectionResult(dto.getInspectionResult());
		existing.setInspectionTimestamp(dto.getInspectionTimestamp());
		return convertToDTO(existing);
	}

	public boolean delete(final int id) {
		return database.delete(id);
	}

	public QualityInspectionDTO inspectCar(final int carId, final String brand, final String color, final boolean inspect) {
		final QualityInspection inspection = new QualityInspection();
		inspection.setCarId(carId);
		inspection.setBrand(brand != null ? brand : "Unknown");
		inspection.setColor(color != null ? color : "Unknown");
		inspection.setInspectionTimestamp(new Date().getTime());

		if (inspect) {
			inspection.setDefectDetected(random.nextBoolean());
			inspection.setDefectType(inspection.isDefectDetected() ? getRandomDefectType() : null);
			inspection.setInspectionResult(inspection.isDefectDetected() ? "DEFECT_DETECTED" : "NO_DEFECT");
		} else {
			inspection.setDefectDetected(false);
			inspection.setDefectType(null);
			inspection.setInspectionResult("NO_INSPECTION");
		}

		final int id = database.create(inspection);
		inspection.setId(id);
		return convertToDTO(inspection);
	}

	private String getRandomDefectType() {
		final String[] defectTypes = {"PAINT_DAMAGE", "DENT", "SCRATCH", "RUST", "CRACK"};
		return defectTypes[random.nextInt(defectTypes.length)];
	}

	private QualityInspectionDTO convertToDTO(final QualityInspection inspection) {
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setId(inspection.getId());
		dto.setCarId(inspection.getCarId());
		dto.setBrand(inspection.getBrand());
		dto.setColor(inspection.getColor());
		dto.setDefectDetected(inspection.isDefectDetected());
		dto.setDefectType(inspection.getDefectType());
		dto.setInspectionResult(inspection.getInspectionResult());
		dto.setInspectionTimestamp(inspection.getInspectionTimestamp());
		return dto;
	}
}
