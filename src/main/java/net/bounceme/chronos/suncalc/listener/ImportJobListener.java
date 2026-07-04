package net.bounceme.chronos.suncalc.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.bounceme.chronos.notifications.services.NotificationService;

@Component
public class ImportJobListener extends AbstractListener {
	
	@Autowired
	private NotificationService notificationService;
	
	/**
	 *
	 */
	protected void initializeConfig(JobExecution jobExecution) {
		Map<String, Long> stepTimes = new HashMap<>();
		
		jobExecution.getExecutionContext().put("STEP_TIMES", stepTimes);
	}

	@Override
	protected void updateStatus(JobExecution jobExecution) {
		Boolean alreadyExecuted = (Boolean) jobExecution.getExecutionContext().get("ALREADY_EXECUTED");
		
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			if (!Objects.isNull(alreadyExecuted) && Boolean.TRUE.equals(alreadyExecuted)) {
				jobExecution.setExitStatus(new ExitStatus("NOOP", "La tarea ya ha sido ejecutada"));
				notificationService.sendNotification("suncalc-batch", "La tarea ya ha sido ejecutada", "WARNING");
			}
			else {
				jobExecution.setExitStatus(new ExitStatus("COMPLETED", "La tarea ha sido ejecutada correctamente"));
				notificationService.sendNotification("suncalc-batch", "La tarea ha sido ejecutada correctamente", "OK");
			}
		}
	}
}
