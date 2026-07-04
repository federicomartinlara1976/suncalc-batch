package net.bounceme.chronos.suncalc.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.bounceme.chronos.suncalc.model.SunriseSunsetResponse;

@FeignClient(name = "sunriseSunsetClient", url = "${application.importTimes.url}")
public interface SunriseSunsetRest {
	
	@GetMapping("/json")
	SunriseSunsetResponse detalle(
			@RequestParam Float lat, 
			@RequestParam Float lng, 
			@RequestParam(required=false) String date,
			@RequestParam(required=false) String tzid,
			@RequestParam(required=false, defaultValue = "1") Integer formatted,
			@RequestParam(required=false) String callback);
}
