package net.bounceme.chronos.suncalc.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;

@Component
public class PeriodicTasks {
	
	@Autowired
	private JobFacade jobFacade;
	
	@Scheduled(cron = "${application.importTimes.cron}")
    public void importTimesTask() {
		sendJob("importTimes");
    }
	
	@Scheduled(cron = "${application.moonPhase.cron}")
    public void importMoonDataTask() {
		sendJob("importMoonData");
    }

	private void sendJob(String name) {
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(TaskDTO.builder().name(name).build());
		
		jobFacade.publishJob(jobDTO);
	}
}
