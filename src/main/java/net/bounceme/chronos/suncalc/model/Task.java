package net.bounceme.chronos.suncalc.model;

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
public class Task implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6557119505034879817L;
	
	@NotEmpty(message = "no puede estar vacío")
	@Getter
	@Setter
	private String name;

}
