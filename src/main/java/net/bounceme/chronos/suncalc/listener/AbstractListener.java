package net.bounceme.chronos.suncalc.listener;

import java.util.Objects;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractListener implements JobExecutionListener {

	private Long startTime;

	@Override
	public void beforeJob(JobExecution jobExecution) {
		initializeConfig(jobExecution);
		
		log.info("JOBLISTENER: Se va a ejecutar el Job con ID: {}", jobExecution.getJobId());
		startTime = System.currentTimeMillis();
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (!Objects.isNull(startTime)) {
			Long duration = System.currentTimeMillis() - startTime;
			log.info("JOBLISTENER: Se ha terminado de ejecutar el Job con ID: {}, ha tardado {} ms",
					jobExecution.getJobId(), duration);
		}
		
		updateStatus(jobExecution);
	}
	
	protected abstract void initializeConfig(JobExecution jobExecution);
	
	protected abstract void updateStatus(JobExecution jobExecution);
}
