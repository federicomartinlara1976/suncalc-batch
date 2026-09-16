package net.bounceme.chronos.suncalc.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;

@Configuration
@Slf4j
@EnableScheduling
public class PeriodicTasks {
	
	@Autowired
	private JobFacade jobFacade;
	
	@Scheduled(cron = "${application.importTimes.cron}")
    public void importJobTask() {
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(TaskDTO.builder().name("importTimes").build());
		
		jobFacade.publishJob(jobDTO);
    }
}
