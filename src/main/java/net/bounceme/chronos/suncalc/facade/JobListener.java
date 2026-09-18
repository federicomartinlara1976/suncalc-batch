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

		String name = (String) content.get("name");

		switch (name) {
		case "importTimes", "recalculateDifferences":
			resultado = jobService.run(name);
			break;
		case "importByMonth": {
			Integer year = (Integer) content.get("year");
			Integer month = (Integer) content.get("month");
			resultado = jobService.run(name, year, month);
		}
			break;
		case "importFromDate": {
			String date = (String) content.get("date");
			resultado = jobService.run(name, date);
		}
			break;
		case "recalculateByYear": {
			Integer year = (Integer) content.get("year");
			resultado = jobService.run(name, year);
		}
			break;
		default:
			log.warn("Tarea no especificada");
		}

		log.info("{}", resultado);
	}

}
