package net.bounceme.chronos.suncalc.model;

import lombok.Getter;
import lombok.Setter;

public class SunriseSunsetResponse {
	
	@Getter
	@Setter
	private SunriseSunsetResult results;
	
	@Getter
	@Setter
	private String status;
	
	@Getter
	@Setter
	private String tzid;
}
