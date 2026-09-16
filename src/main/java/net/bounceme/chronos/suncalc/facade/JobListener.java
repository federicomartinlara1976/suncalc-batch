package net.bounceme.chronos.suncalc.facade;

import java.util.Map;

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

		if (content.containsKey("name")) {
			resultado = jobService.run((String) content.get("name"));
		}

		if (content.containsKey("year")) {
			resultado = jobService.runImportByMonthAndYear((Integer) content.get("year"),
					(Integer) content.get("month"));
		}
		
		log.info("{}", resultado);
	}

}
