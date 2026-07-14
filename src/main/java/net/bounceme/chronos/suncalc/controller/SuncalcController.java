package net.bounceme.chronos.suncalc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;
import net.bounceme.chronos.suncalc.validation.validator.Fecha;

@RestController
@RequestMapping("/suncalc-batch/")
@Slf4j
public class SuncalcController {

	@Autowired
	private SuncalcService suncalcService;
	
	@GetMapping("current")
	public ResponseEntity<Map<String, Object>> getCurrent() {
		Map<String, Object> response = new HashMap<>();

		TimeData timeData = suncalcService.getCurrentTimeData();
		response.put("result", timeData);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@GetMapping("fecha")
	public ResponseEntity<Map<String, Object>> getByDate(@RequestParam("d") @Fecha String date) {
		return suncalcService.getTimeDataByDate(date).map(t -> {
			Map<String, Object> response = new HashMap<>();
			response.put("result", t);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}
	
	@GetMapping("fechas")
	public ResponseEntity<Map<String, Object>> getByDates(@RequestParam("inicio") @Fecha String initDate, @RequestParam("fin") @Fecha String endDate) {
		Map<String, Object> response = new HashMap<>();
		List<TimeData> registros = suncalcService.getByRangeDate(initDate, endDate);
		
		if (CollectionUtils.isNotEmpty(registros)) {
			response.put("result", registros);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
