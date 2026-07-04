package net.bounceme.chronos.suncalc.model;

import lombok.Getter;
import lombok.Setter;

public class SunriseSunsetResult {

	@Getter
	@Setter
	private String sunrise;
	
	@Getter
	@Setter
	private String sunset;
	
	@Getter
	@Setter
	private String solar_noon;
	
	@Getter
	@Setter
	private Long day_length;
	
	@Getter
	@Setter
	private String civil_twilight_begin;
	
	@Getter
	@Setter
	private String civil_twilight_end;
	
	@Getter
	@Setter
	private String nautical_twilight_begin;
	
	@Getter
	@Setter
	private String nautical_twilight_end;
	
	@Getter
	@Setter
	private String astronomical_twilight_begin;
	
	@Getter
	@Setter
	private String astronomical_twilight_end;
}
