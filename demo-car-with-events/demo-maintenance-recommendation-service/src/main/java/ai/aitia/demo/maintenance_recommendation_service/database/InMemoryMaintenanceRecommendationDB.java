package ai.aitia.demo.maintenance_recommendation_service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import ai.aitia.demo.maintenance_recommendation_service.entity.MaintenanceRecommendation;

@Component
public class InMemoryMaintenanceRecommendationDB {

	private final Map<Integer, MaintenanceRecommendation> database = new ConcurrentHashMap<>();
	private final AtomicInteger idGenerator = new AtomicInteger(0);

	public synchronized int create(final MaintenanceRecommendation recommendation) {
		final int id = idGenerator.incrementAndGet();
		recommendation.setId(id);
		database.put(id, recommendation);
		return id;
	}

	public MaintenanceRecommendation getById(final int id) {
		return database.get(id);
	}

	public List<MaintenanceRecommendation> getAll() {
		return new ArrayList<>(database.values());
	}

	public List<MaintenanceRecommendation> getByCarId(final int carId) {
		final List<MaintenanceRecommendation> result = new ArrayList<>();
		for (final MaintenanceRecommendation recommendation : database.values()) {
			if (recommendation.getCarId() == carId) {
				result.add(recommendation);
			}
		}
		return result;
	}

	public boolean delete(final int id) {
		return database.remove(id) != null;
	}

	public List<MaintenanceRecommendation> search(final Map<String, String> params) {
		final List<MaintenanceRecommendation> result = new ArrayList<>();
		for (final MaintenanceRecommendation recommendation : database.values()) {
			if (matchesCriteria(recommendation, params)) {
				result.add(recommendation);
			}
		}
		return result;
	}

	private boolean matchesCriteria(final MaintenanceRecommendation recommendation, final Map<String, String> params) {
		for (final Map.Entry<String, String> param : params.entrySet()) {
			switch (param.getKey()) {
				case "car-id":
					if (recommendation.getCarId() != Integer.parseInt(param.getValue())) {
						return false;
					}
					break;
				case "brand":
					if (!recommendation.getBrand().equals(param.getValue())) {
						return false;
					}
					break;
				case "priority":
					if (!recommendation.getPriority().equals(param.getValue())) {
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
