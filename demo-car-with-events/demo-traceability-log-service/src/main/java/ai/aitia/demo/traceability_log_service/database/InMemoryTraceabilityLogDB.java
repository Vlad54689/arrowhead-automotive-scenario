package ai.aitia.demo.traceability_log_service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import ai.aitia.demo.traceability_log_service.entity.TraceabilityLog;

@Component
public class InMemoryTraceabilityLogDB {

	private final Map<Integer, TraceabilityLog> database = new ConcurrentHashMap<>();
	private final AtomicInteger idGenerator = new AtomicInteger(0);

	public synchronized int create(final TraceabilityLog log) {
		final int id = idGenerator.incrementAndGet();
		log.setId(id);
		database.put(id, log);
		return id;
	}

	public TraceabilityLog getById(final int id) {
		return database.get(id);
	}

	public List<TraceabilityLog> getAll() {
		return new ArrayList<>(database.values());
	}

	public List<TraceabilityLog> getByCarId(final int carId) {
		final List<TraceabilityLog> result = new ArrayList<>();
		for (final TraceabilityLog log : database.values()) {
			if (log.getCarId() == carId) {
				result.add(log);
			}
		}
		return result;
	}

	public boolean delete(final int id) {
		return database.remove(id) != null;
	}

	public List<TraceabilityLog> search(final Map<String, String> params) {
		final List<TraceabilityLog> result = new ArrayList<>();
		for (final TraceabilityLog log : database.values()) {
			if (matchesCriteria(log, params)) {
				result.add(log);
			}
		}
		return result;
	}

	private boolean matchesCriteria(final TraceabilityLog log, final Map<String, String> params) {
		for (final Map.Entry<String, String> param : params.entrySet()) {
			switch (param.getKey()) {
				case "car-id":
					if (log.getCarId() != Integer.parseInt(param.getValue())) {
						return false;
					}
					break;
				case "brand":
					if (!log.getBrand().equals(param.getValue())) {
						return false;
					}
					break;
				case "event-type":
					if (!log.getEventType().equals(param.getValue())) {
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
