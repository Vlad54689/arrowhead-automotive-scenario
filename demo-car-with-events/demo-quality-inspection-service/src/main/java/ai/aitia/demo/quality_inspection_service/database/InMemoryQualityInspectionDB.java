package ai.aitia.demo.quality_inspection_service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import ai.aitia.demo.quality_inspection_service.entity.QualityInspection;

@Component
public class InMemoryQualityInspectionDB {

	private final Map<Integer, QualityInspection> database = new ConcurrentHashMap<>();
	private final AtomicInteger idGenerator = new AtomicInteger(0);

	public synchronized int create(final QualityInspection inspection) {
		final int id = idGenerator.incrementAndGet();
		inspection.setId(id);
		database.put(id, inspection);
		return id;
	}

	public QualityInspection getById(final int id) {
		return database.get(id);
	}

	public List<QualityInspection> getAll() {
		return new ArrayList<>(database.values());
	}

	public List<QualityInspection> getByCarId(final int carId) {
		final List<QualityInspection> result = new ArrayList<>();
		for (final QualityInspection inspection : database.values()) {
			if (inspection.getCarId() == carId) {
				result.add(inspection);
			}
		}
		return result;
	}

	public boolean delete(final int id) {
		return database.remove(id) != null;
	}

	public List<QualityInspection> search(final Map<String, String> params) {
		final List<QualityInspection> result = new ArrayList<>();
		for (final QualityInspection inspection : database.values()) {
			if (matchesCriteria(inspection, params)) {
				result.add(inspection);
			}
		}
		return result;
	}

	private boolean matchesCriteria(final QualityInspection inspection, final Map<String, String> params) {
		for (final Map.Entry<String, String> param : params.entrySet()) {
			switch (param.getKey()) {
				case "car-id":
					if (inspection.getCarId() != Integer.parseInt(param.getValue())) {
						return false;
					}
					break;
				case "brand":
					if (!inspection.getBrand().equals(param.getValue())) {
						return false;
					}
					break;
				case "color":
					if (!inspection.getColor().equals(param.getValue())) {
						return false;
					}
					break;
				case "defect-detected":
					if (inspection.isDefectDetected() != Boolean.parseBoolean(param.getValue())) {
						return false;
					}
					break;
				default:
					return false;
			}
		}
		return true;
	}
}
