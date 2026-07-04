package net.bounceme.chronos.suncalc.model;

import org.springframework.batch.core.ExitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class ExecutionResult {
	
	private ExitStatus exitStatus;
	
	private String message;
}
