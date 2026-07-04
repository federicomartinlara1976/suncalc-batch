package net.bounceme.chronos.suncalc.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;

@RestController
@RequestMapping("/suncalc-batch/")
@Slf4j
public class SuncalcController {

	@Autowired
	private SuncalcService suncalcService;

	@GetMapping("current")
	public ResponseEntity<Map<String, Object>> getJobs() {
		Map<String, Object> response = new HashMap<>();

		TimeData timeData = suncalcService.getCurrentTimeData();
		response.put("result", timeData);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
