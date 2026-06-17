package ai.aitia.demo.cold_chain_monitor_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.demo.cold_chain_monitor_service.ColdChainMonitorConstants;
import ai.aitia.demo.cold_chain_monitor_service.dto.ColdChainReadingDTO;
import ai.aitia.demo.cold_chain_monitor_service.service.ColdChainMonitorService;

@RestController
@RequestMapping(ColdChainMonitorConstants.READING_URI)
public class ColdChainMonitorController {

	@Autowired
	private ColdChainMonitorService coldChainMonitorService;

	//-------------------------------------------------------------------------------------------------
	@PostMapping
	public ResponseEntity<ColdChainReadingDTO> ingest(@RequestBody final ColdChainReadingDTO dto) {
		final ColdChainReadingDTO accepted = coldChainMonitorService.ingest(dto);
		return new ResponseEntity<>(accepted, HttpStatus.ACCEPTED);
	}
}
