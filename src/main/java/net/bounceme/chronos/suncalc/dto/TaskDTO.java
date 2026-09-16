package net.bounceme.chronos.suncalc.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotEmpty;
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
public class TaskDTO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6557119505034879817L;
	
	@NotEmpty(message = "no puede estar vacío")
	@Getter
	@Setter
	private String name;
	
	@Getter
	@Setter
	private Integer year;
	
	@Getter
	@Setter
	private Integer month;

}
