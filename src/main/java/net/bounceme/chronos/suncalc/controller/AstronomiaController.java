package net.bounceme.chronos.suncalc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.services.AstronomiaService;

@RestController
@RequestMapping("/suncalc-batch/solstice-equinox")
@Slf4j
public class AstronomiaController {

	@Autowired
	private AstronomiaService astronomiaService;
	
	@GetMapping("currentYear")
	public ResponseEntity<Map<String, Object>> getCurrent() {
		Map<String, Object> response = new HashMap<>();

		List<DataSolsticeEquinoxDTO> results = astronomiaService.calculateSolsticesEquinoxes(2026);
		response.put("result", results);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
