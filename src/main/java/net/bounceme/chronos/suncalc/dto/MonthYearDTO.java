package net.bounceme.chronos.suncalc.dto;

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
public class MonthYearDTO {

	@Getter
	@Setter
	private Integer year;
	
	@Getter
	@Setter
	private Integer month;
}
