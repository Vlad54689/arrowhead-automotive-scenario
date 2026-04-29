package ai.aitia.demo.traceability_log_service.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.demo.traceability_log_service.database.InMemoryTraceabilityLogDB;
import ai.aitia.demo.traceability_log_service.dto.TraceabilityLogDTO;
import ai.aitia.demo.traceability_log_service.entity.TraceabilityLog;

@Service
public class TraceabilityLogService {

	@Autowired
	private InMemoryTraceabilityLogDB database;

	private final Random random = new Random();

	public TraceabilityLog create(final TraceabilityLogDTO dto) {
		final TraceabilityLog log = new TraceabilityLog();
		log.setCarId(dto.getCarId());
		log.setBrand(dto.getBrand());
		log.setEventType(dto.getEventType());
		log.setEventDetails(dto.getEventDetails());
		log.setEventTimestamp(dto.getEventTimestamp());
		final int id = database.create(log);
		log.setId(id);
		return log;
	}

	public TraceabilityLogDTO getById(final int id) {
		final TraceabilityLog log = database.getById(id);
		return log != null ? convertToDTO(log) : null;
	}

	public List<TraceabilityLogDTO> getAll() {
		final List<TraceabilityLog> logs = database.getAll();
		final List<TraceabilityLogDTO> dtos = new ArrayList<>();
		for (final TraceabilityLog log : logs) {
			dtos.add(convertToDTO(log));
		}
		return dtos;
	}

	public List<TraceabilityLogDTO> getByCarId(final int carId) {
		final List<TraceabilityLog> logs = database.getByCarId(carId);
		final List<TraceabilityLogDTO> dtos = new ArrayList<>();
		for (final TraceabilityLog log : logs) {
			dtos.add(convertToDTO(log));
		}
		return dtos;
	}

	public List<TraceabilityLogDTO> search(final Map<String, String> params) {
		final List<TraceabilityLog> logs = database.search(params);
		final List<TraceabilityLogDTO> dtos = new ArrayList<>();
		for (final TraceabilityLog log : logs) {
			dtos.add(convertToDTO(log));
		}
		return dtos;
	}

	public TraceabilityLogDTO update(final int id, final TraceabilityLogDTO dto) {
		final TraceabilityLog existing = database.getById(id);
		if (existing == null) {
			return null;
		}
		existing.setCarId(dto.getCarId());
		existing.setBrand(dto.getBrand());
		existing.setEventType(dto.getEventType());
		existing.setEventDetails(dto.getEventDetails());
		existing.setEventTimestamp(dto.getEventTimestamp());
		return convertToDTO(existing);
	}

	public boolean delete(final int id) {
		return database.delete(id);
	}

	public TraceabilityLogDTO logEvent(final int carId, final String brand, final String eventType) {
		final TraceabilityLog log = new TraceabilityLog();
		log.setCarId(carId);
		log.setBrand(brand != null ? brand : "Unknown");
		log.setEventType(eventType != null ? eventType : "UNKNOWN_EVENT");
		log.setEventTimestamp(new Date().getTime());

		final String details = generateEventDetails(eventType);
		log.setEventDetails(details);

		final int id = database.create(log);
		log.setId(id);
		return convertToDTO(log);
	}

	private String generateEventDetails(final String eventType) {
		final StringBuilder details = new StringBuilder();
		details.append("Event: ");
		details.append(eventType != null ? eventType : "UNKNOWN");
		details.append(" - Processed at ");
		details.append(new Date().toString());
		return details.toString();
	}

	private TraceabilityLogDTO convertToDTO(final TraceabilityLog log) {
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setId(log.getId());
		dto.setCarId(log.getCarId());
		dto.setBrand(log.getBrand());
		dto.setEventType(log.getEventType());
		dto.setEventDetails(log.getEventDetails());
		dto.setEventTimestamp(log.getEventTimestamp());
		return dto;
	}
}
