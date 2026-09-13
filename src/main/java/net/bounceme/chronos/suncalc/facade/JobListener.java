package net.bounceme.chronos.suncalc.facade;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;

@Component
@Slf4j
public class JobListener {
	
	@EventListener
	public void executeJob(JobDTO<?> jobDTO) {
		log.info("Execute with: {}", jobDTO);
	}

}
