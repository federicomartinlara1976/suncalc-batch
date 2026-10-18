package net.bounceme.chronos.suncalc.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FaseLunarDTO {
	
	@Getter
	@Setter
	private Date date;
	
	@Getter
	@Setter
	private String weekday;

	@Getter
	@Setter
	private String phaseName;
	
	@Getter
	@Setter
	private String phaseEmoji;

	@Getter
	@Setter
	private Float illuminationPercent;

	@Getter
	@Setter
	private Float moonAgeDays;

	@Getter
	@Setter
	private Float distanceKm;
        
	@Getter
	@Setter
	private Date principalPhase;
        
	@Getter
	@Setter
	private Date principalPhaseTime;
        
	@Getter
	@Setter
	private Date moonrise;
        
	@Getter
	@Setter
	private Date moonset;

}
