package ai.aitia.demo.quality_inspection_service.controller;

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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.quality_inspection_service.QualityInspectionConstants;
import ai.aitia.demo.quality_inspection_service.dto.QualityInspectionDTO;
import ai.aitia.demo.quality_inspection_service.entity.QualityInspection;
import ai.aitia.demo.quality_inspection_service.service.QualityInspectionService;

@RestController
@RequestMapping(QualityInspectionConstants.CAR_URI)
public class QualityInspectionController {

	@Autowired
	private QualityInspectionService qualityInspectionService;

	@PostMapping
	public ResponseEntity<QualityInspectionDTO> create(@RequestBody final QualityInspectionDTO dto) {
		final QualityInspectionDTO inspection = qualityInspectionService.create(dto);
		return new ResponseEntity<>(inspection, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<QualityInspectionDTO>> getAll() {
		final List<QualityInspectionDTO> inspections = qualityInspectionService.getAll();
		return new ResponseEntity<>(inspections, HttpStatus.OK);
	}

	@RequestMapping(value = "/byCarId", method = RequestMethod.GET)
	public ResponseEntity<List<QualityInspectionDTO>> getByCarId(@RequestParam(QualityInspectionConstants.REQUEST_PARAM_CAR_ID) final int carId) {
		final List<QualityInspectionDTO> inspections = qualityInspectionService.getByCarId(carId);
		return new ResponseEntity<>(inspections, HttpStatus.OK);
	}

	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ResponseEntity<List<QualityInspectionDTO>> search(@RequestParam final Map<String, String> params) {
		final List<QualityInspectionDTO> inspections = qualityInspectionService.search(params);
		return new ResponseEntity<>(inspections, HttpStatus.OK);
	}

	@GetMapping(QualityInspectionConstants.BY_ID_PATH)
	public ResponseEntity<QualityInspectionDTO> getById(@PathVariable(QualityInspectionConstants.PATH_VARIABLE_ID) final int id) {
		final QualityInspectionDTO inspection = qualityInspectionService.getById(id);
		if (inspection == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(inspection, HttpStatus.OK);
	}

	@PutMapping(QualityInspectionConstants.BY_ID_PATH)
	public ResponseEntity<QualityInspectionDTO> update(@PathVariable(QualityInspectionConstants.PATH_VARIABLE_ID) final int id,
			@RequestBody final QualityInspectionDTO dto) {
		final QualityInspectionDTO updated = qualityInspectionService.update(id, dto);
		if (updated == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping(QualityInspectionConstants.BY_ID_PATH)
	public ResponseEntity<Void> delete(@PathVariable(QualityInspectionConstants.PATH_VARIABLE_ID) final int id) {
		final boolean deleted = qualityInspectionService.delete(id);
		if (!deleted) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping("/inspect")
	public ResponseEntity<QualityInspectionDTO> inspectCar(@RequestParam("carId") final int carId,
			@RequestParam(required = false, value = "brand") final String brand,
			@RequestParam(required = false, value = "color") final String color,
			@RequestParam(required = false, value = "inspect") final boolean inspect) {
		if (inspect) {
			final QualityInspectionDTO dto = new QualityInspectionDTO();
			dto.setCarId(carId);
			dto.setBrand(brand);
			dto.setColor(color);
			dto.setDefectDetected(true);
			dto.setDefectType("SCRATCH");
			dto.setInspectionResult("DEFECT_DETECTED");
			dto.setInspectionTimestamp(System.currentTimeMillis());
			final QualityInspectionDTO created = qualityInspectionService.create(dto);
			return new ResponseEntity<>(created, HttpStatus.OK);
		}
		final Map<String, String> params = new HashMap<>();
		params.put("car-id", String.valueOf(carId));
		if (brand != null) {
			params.put("brand", brand);
		}
		if (color != null) {
			params.put("color", color);
		}
		final List<QualityInspectionDTO> inspections = qualityInspectionService.search(params);
		if (inspections.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(inspections.get(0), HttpStatus.OK);
	}
}
