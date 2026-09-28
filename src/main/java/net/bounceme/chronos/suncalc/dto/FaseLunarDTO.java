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
	private Float edad;
	
	@Getter
	@Setter
	private String fase;
	
	@Getter
	@Setter
	private Float iluminacion;

}
