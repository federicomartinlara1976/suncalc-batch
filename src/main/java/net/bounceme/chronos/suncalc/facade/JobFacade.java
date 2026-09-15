package net.bounceme.chronos.suncalc.facade;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;

@Component
@Slf4j
public class JobFacade {
	
	@Value("${application.queue}")
	private String queueName;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	public void publishJob(JobDTO<?> jobDTO) {
		rabbitTemplate.convertAndSend(queueName, jobDTO);
	}
}
