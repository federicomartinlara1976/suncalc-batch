package net.bounceme.chronos.suncalc.listener;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;

import net.bounceme.chronos.suncalc.model.Execution;

@Component
public class StatExecutionsListener extends AbstractListener {
	
	/**
	 *
	 */
	protected void initializeConfig(JobExecution jobExecution) {
		List<Execution> executions = new ArrayList<>();
		
		jobExecution.getExecutionContext().put("EXECUTIONS", executions);
	}

	@Override
	protected void updateStatus(JobExecution jobExecution) {
		jobExecution.setExitStatus(new ExitStatus("COMPLETED", "La tarea ha sido ejecutada correctamente"));
	}

}
