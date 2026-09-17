package net.bounceme.chronos.suncalc.facade;

import java.util.Map;
import java.util.Objects;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.services.JobService;

@Component
@Slf4j
public class JobListener {

	@Autowired
	private JobService jobService;

	@SuppressWarnings("unchecked")
	@RabbitListener(queues = "SuncalcEventos")
	public void executeJob(JobDTO<?> jobDTO) {
		ExecutionResult resultado = null;
		Map<String, Object> content = (Map<String, Object>) jobDTO.getContent();

		Integer year = (Integer) content.get("year"); 
		if (!Objects.isNull(year)) {
			Integer month = (Integer) content.get("month");
			
			if (!Objects.isNull(month)) {
				resultado = jobService.run((String) content.get("name"), year, month);
			}
			else {
				resultado = jobService.run((String) content.get("name"), year);
			}
		}
		else {
			resultado = jobService.run((String) content.get("name"));
		}
		
		log.info("{}", resultado);
	}

}
