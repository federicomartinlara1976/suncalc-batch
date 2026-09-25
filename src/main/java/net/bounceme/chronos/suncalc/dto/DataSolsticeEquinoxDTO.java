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
public class DataSolsticeEquinoxDTO {
	
	@Getter
	@Setter
	private String name;
	
	@Getter
	@Setter
	private Date utcDate;
	
	@Getter
	@Setter
	private Date localDate;

}
