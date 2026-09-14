package net.bounceme.chronos.suncalc.model;

import org.springframework.batch.core.ExitStatus;

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
public class ExecutionResult {
	
	@Getter
	@Setter
	private ExitStatus exitStatus;
	
	@Getter
	@Setter
	private String message;
}
