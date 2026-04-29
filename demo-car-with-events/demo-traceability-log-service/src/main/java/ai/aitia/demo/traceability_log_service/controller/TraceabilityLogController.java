package ai.aitia.demo.traceability_log_service.controller;

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

import ai.aitia.demo.traceability_log_service.TraceabilityLogConstants;
import ai.aitia.demo.traceability_log_service.dto.TraceabilityLogDTO;
import ai.aitia.demo.traceability_log_service.entity.TraceabilityLog;
import ai.aitia.demo.traceability_log_service.service.TraceabilityLogService;

@RestController
@RequestMapping(TraceabilityLogConstants.CAR_URI)
public class TraceabilityLogController {

	@Autowired
	private TraceabilityLogService traceabilityLogService;

	@PostMapping
	public ResponseEntity<TraceabilityLogDTO> create(@RequestBody final TraceabilityLogDTO dto) {
		final TraceabilityLog log = traceabilityLogService.create(dto);
		return new ResponseEntity<>(convertToDTO(log), HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<TraceabilityLogDTO>> getAll() {
		final List<TraceabilityLogDTO> logs = traceabilityLogService.getAll();
		return new ResponseEntity<>(logs, HttpStatus.OK);
	}

	@GetMapping("/by-car-id")
	public ResponseEntity<List<TraceabilityLogDTO>> getByCarId(@RequestParam(TraceabilityLogConstants.REQUEST_PARAM_CAR_ID) final int carId) {
		final List<TraceabilityLogDTO> logs = traceabilityLogService.getByCarId(carId);
		return new ResponseEntity<>(logs, HttpStatus.OK);
	}

	@GetMapping("/search")
	public ResponseEntity<List<TraceabilityLogDTO>> search(@RequestParam final Map<String, String> params) {
		final List<TraceabilityLogDTO> logs = traceabilityLogService.search(params);
		return new ResponseEntity<>(logs, HttpStatus.OK);
	}

	@GetMapping(TraceabilityLogConstants.BY_ID_PATH)
	public ResponseEntity<TraceabilityLogDTO> getById(@PathVariable(TraceabilityLogConstants.PATH_VARIABLE_ID) final int id) {
		final TraceabilityLogDTO log = traceabilityLogService.getById(id);
		if (log == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(log, HttpStatus.OK);
	}

	@PutMapping(TraceabilityLogConstants.BY_ID_PATH)
	public ResponseEntity<TraceabilityLogDTO> update(@PathVariable(TraceabilityLogConstants.PATH_VARIABLE_ID) final int id,
			@RequestBody final TraceabilityLogDTO dto) {
		final TraceabilityLogDTO updated = traceabilityLogService.update(id, dto);
		if (updated == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping(TraceabilityLogConstants.BY_ID_PATH)
	public ResponseEntity<Void> delete(@PathVariable(TraceabilityLogConstants.PATH_VARIABLE_ID) final int id) {
		final boolean deleted = traceabilityLogService.delete(id);
		if (!deleted) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping("/log")
	public ResponseEntity<TraceabilityLogDTO> logEvent(@RequestParam(TraceabilityLogConstants.REQUEST_PARAM_CAR_ID) final int carId,
			@RequestParam(required = false, name = TraceabilityLogConstants.REQUEST_PARAM_BRAND) final String brand,
			@RequestParam(required = false, name = TraceabilityLogConstants.REQUEST_PARAM_EVENT_TYPE) final String eventType) {
		final Map<String, String> params = new HashMap<>();
		params.put(TraceabilityLogConstants.REQUEST_PARAM_CAR_ID, String.valueOf(carId));
		if (brand != null) {
			params.put(TraceabilityLogConstants.REQUEST_PARAM_BRAND, brand);
		}
		if (eventType != null) {
			params.put(TraceabilityLogConstants.REQUEST_PARAM_EVENT_TYPE, eventType);
		}
		final List<TraceabilityLogDTO> logs = traceabilityLogService.search(params);
		if (logs.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(logs.get(0), HttpStatus.OK);
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
