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
public class InputFaseLunarDTO {

	@Getter
	@Setter
	private Date startDate;
    
	@Getter
	@Setter
	private Integer days;
    
	@Getter
	@Setter
	private Float latitude;
    
	@Getter
	@Setter
	private Float longitude;
    
	@Getter
	@Setter
	private String timezone;
}
