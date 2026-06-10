package ai.aitia.demo.maintenance_recommendation_service.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.demo.maintenance_recommendation_service.database.InMemoryMaintenanceRecommendationDB;
import ai.aitia.demo.maintenance_recommendation_service.dto.MaintenanceRecommendationDTO;
import ai.aitia.demo.maintenance_recommendation_service.entity.MaintenanceRecommendation;

@Service
public class MaintenanceRecommendationService {

	@Autowired
	private InMemoryMaintenanceRecommendationDB database;

	private final Random random = new Random();

	public MaintenanceRecommendationDTO create(final MaintenanceRecommendationDTO dto) {
		final MaintenanceRecommendation recommendation = new MaintenanceRecommendation();
		recommendation.setCarId(dto.getCarId());
		recommendation.setBrand(dto.getBrand());
		recommendation.setRecommendationType(dto.getRecommendationType());
		recommendation.setRecommendationDetails(dto.getRecommendationDetails());
		recommendation.setPriority(dto.getPriority());
		recommendation.setRecommendationTimestamp(dto.getRecommendationTimestamp());
		final int id = database.create(recommendation);
		recommendation.setId(id);
		return convertToDTO(recommendation);
	}

	public MaintenanceRecommendationDTO getById(final int id) {
		final MaintenanceRecommendation recommendation = database.getById(id);
		return recommendation != null ? convertToDTO(recommendation) : null;
	}

	public List<MaintenanceRecommendationDTO> getAll() {
		final List<MaintenanceRecommendation> recommendations = database.getAll();
		final List<MaintenanceRecommendationDTO> dtos = new ArrayList<>();
		for (final MaintenanceRecommendation recommendation : recommendations) {
			dtos.add(convertToDTO(recommendation));
		}
		return dtos;
	}

	public List<MaintenanceRecommendationDTO> getByCarId(final int carId) {
		final List<MaintenanceRecommendation> recommendations = database.getByCarId(carId);
		final List<MaintenanceRecommendationDTO> dtos = new ArrayList<>();
		for (final MaintenanceRecommendation recommendation : recommendations) {
			dtos.add(convertToDTO(recommendation));
		}
		return dtos;
	}

	public List<MaintenanceRecommendationDTO> search(final Map<String, String> params) {
		final List<MaintenanceRecommendation> recommendations = database.search(params);
		final List<MaintenanceRecommendationDTO> dtos = new ArrayList<>();
		for (final MaintenanceRecommendation recommendation : recommendations) {
			dtos.add(convertToDTO(recommendation));
		}
		return dtos;
	}

	public MaintenanceRecommendationDTO update(final int id, final MaintenanceRecommendationDTO dto) {
		final MaintenanceRecommendation existing = database.getById(id);
		if (existing == null) {
			return null;
		}
		existing.setCarId(dto.getCarId());
		existing.setBrand(dto.getBrand());
		existing.setRecommendationType(dto.getRecommendationType());
		existing.setRecommendationDetails(dto.getRecommendationDetails());
		existing.setPriority(dto.getPriority());
		existing.setRecommendationTimestamp(dto.getRecommendationTimestamp());
		return convertToDTO(existing);
	}

	public boolean delete(final int id) {
		return database.delete(id);
	}

	public MaintenanceRecommendationDTO recommendCar(final int carId, final String brand, final String priority) {
		final MaintenanceRecommendation recommendation = new MaintenanceRecommendation();
		recommendation.setCarId(carId);
		recommendation.setBrand(brand != null ? brand : "Unknown");
		recommendation.setPriority(priority != null ? priority : "MEDIUM");
		recommendation.setRecommendationTimestamp(new Date().getTime());

		final String type = getRandomRecommendationType();
		recommendation.setRecommendationType(type);
		recommendation.setRecommendationDetails(getRecommendationDetails(type, priority));

		final int id = database.create(recommendation);
		recommendation.setId(id);
		return convertToDTO(recommendation);
	}

	private String getRandomRecommendationType() {
		final String[] types = {"OIL_CHANGE", "TIRE_ROTATION", "BRAKE_INSPECTION", "FILTER_REPLACEMENT", "BATTERY_CHECK"};
		return types[random.nextInt(types.length)];
	}

	private String getRecommendationDetails(final String type, final String priority) {
		final StringBuilder details = new StringBuilder();
		details.append("Recommended ");
		details.append(type.toLowerCase().replace("_", " "));
		details.append(" with ");
		details.append(priority.toLowerCase());
		details.append(" priority.");
		return details.toString();
	}

	private MaintenanceRecommendationDTO convertToDTO(final MaintenanceRecommendation recommendation) {
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setId(recommendation.getId());
		dto.setCarId(recommendation.getCarId());
		dto.setBrand(recommendation.getBrand());
		dto.setRecommendationType(recommendation.getRecommendationType());
		dto.setRecommendationDetails(recommendation.getRecommendationDetails());
		dto.setPriority(recommendation.getPriority());
		dto.setRecommendationTimestamp(recommendation.getRecommendationTimestamp());
		return dto;
	}
}
