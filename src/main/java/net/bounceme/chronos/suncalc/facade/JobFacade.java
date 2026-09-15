package net.bounceme.chronos.suncalc.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;

@Component
@Slf4j
public class JobFacade {
	
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	
	public void publishJob(JobDTO<?> jobDTO) {
		applicationEventPublisher.publishEvent(jobDTO);
	}
}
