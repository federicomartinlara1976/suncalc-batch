package net.bounceme.chronos.suncalc.scheduler;

import java.util.Date;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@EnableScheduling
public class PeriodicTasks {
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	@Qualifier("importTimes")
	private Job importTimes;

	@Scheduled(cron = "${application.importTimes.cron}")
    public void importJobTask() {
		executeJob(importTimes);
    }
	
	/**
	 * @param job
	 */
	private void executeJob(Job job) {
		try {
	        JobParametersBuilder builder = new JobParametersBuilder();
			builder.addDate("date", new Date());
			
			JobExecution result = jobLauncher.run(job, builder.toJobParameters());
			
			// Exit on failure
			if (ExitStatus.FAILED.equals(result.getExitStatus())) {
				log.error("{} failed!", job.getName());
			}
			
			if (ExitStatus.COMPLETED.equals(result.getExitStatus())) {
				log.info("{} completed", job.getName());
			}
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			log.error("ERROR:", e);
		}
    }
}
