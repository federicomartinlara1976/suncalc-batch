package net.bounceme.chronos.suncalc.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;

@Component
@EnableScheduling
public class PeriodicTasks {
	
	@Autowired
	private JobFacade jobFacade;
	
	@Scheduled(cron = "${application.importTimes.cron}")
    public void importTimesTask() {
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(TaskDTO.builder().name("importTimes").build());
		
		jobFacade.publishJob(jobDTO);
    }
}
