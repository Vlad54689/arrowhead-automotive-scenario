package ai.aitia.demo.maintenance_recommendation_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.maintenance_recommendation_service.MaintenanceRecommendationConstants;
import ai.aitia.demo.maintenance_recommendation_service.dto.MaintenanceRecommendationDTO;
import ai.aitia.demo.maintenance_recommendation_service.entity.MaintenanceRecommendation;
import ai.aitia.demo.maintenance_recommendation_service.service.MaintenanceRecommendationService;

@RestController
@RequestMapping(MaintenanceRecommendationConstants.CAR_URI)
public class MaintenanceRecommendationController {

	@Autowired
	private MaintenanceRecommendationService maintenanceRecommendationService;

	@PostMapping
	public ResponseEntity<MaintenanceRecommendationDTO> create(@RequestBody final MaintenanceRecommendationDTO dto) {
		final MaintenanceRecommendationDTO recommendation = maintenanceRecommendationService.create(dto);
		return new ResponseEntity<>(recommendation, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<MaintenanceRecommendationDTO>> getAll() {
		final List<MaintenanceRecommendationDTO> recommendations = maintenanceRecommendationService.getAll();
		return new ResponseEntity<>(recommendations, HttpStatus.OK);
	}

	@GetMapping("/by-car-id")
	public ResponseEntity<List<MaintenanceRecommendationDTO>> getByCarId(@RequestParam(MaintenanceRecommendationConstants.REQUEST_PARAM_CAR_ID) final int carId) {
		final List<MaintenanceRecommendationDTO> recommendations = maintenanceRecommendationService.getByCarId(carId);
		return new ResponseEntity<>(recommendations, HttpStatus.OK);
	}

	@GetMapping("/search")
	public ResponseEntity<List<MaintenanceRecommendationDTO>> search(@RequestParam final Map<String, String> params) {
		final List<MaintenanceRecommendationDTO> recommendations = maintenanceRecommendationService.search(params);
		return new ResponseEntity<>(recommendations, HttpStatus.OK);
	}

	@GetMapping(MaintenanceRecommendationConstants.BY_ID_PATH)
	public ResponseEntity<MaintenanceRecommendationDTO> getById(@PathVariable(MaintenanceRecommendationConstants.PATH_VARIABLE_ID) final int id) {
		final MaintenanceRecommendationDTO recommendation = maintenanceRecommendationService.getById(id);
		if (recommendation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(recommendation, HttpStatus.OK);
	}

	@PutMapping(MaintenanceRecommendationConstants.BY_ID_PATH)
	public ResponseEntity<MaintenanceRecommendationDTO> update(@PathVariable(MaintenanceRecommendationConstants.PATH_VARIABLE_ID) final int id,
			@RequestBody final MaintenanceRecommendationDTO dto) {
		final MaintenanceRecommendationDTO updated = maintenanceRecommendationService.update(id, dto);
		if (updated == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping(MaintenanceRecommendationConstants.BY_ID_PATH)
	public ResponseEntity<Void> delete(@PathVariable(MaintenanceRecommendationConstants.PATH_VARIABLE_ID) final int id) {
		final boolean deleted = maintenanceRecommendationService.delete(id);
		if (!deleted) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping("/recommend")
	public ResponseEntity<MaintenanceRecommendationDTO> recommend(@RequestParam(MaintenanceRecommendationConstants.REQUEST_PARAM_CAR_ID) final int carId,
			@RequestParam(name = "brand", required = false) final String brand,
			@RequestParam(name = "priority", required = false) final String priority) {
		final MaintenanceRecommendationDTO recommendation = maintenanceRecommendationService.recommendCar(carId, brand, priority);
		return new ResponseEntity<>(recommendation, HttpStatus.OK);
	}
}
