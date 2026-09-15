package net.bounceme.chronos.suncalc.facade;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.MonthYearDTO;
import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.model.Task;
import net.bounceme.chronos.suncalc.services.JobService;

@Component
@Slf4j
public class JobListener {
	
	@Autowired
	private JobService jobService;
	
	@RabbitListener(queues = "SuncalcEventos")
	public void executeJob(JobDTO<?> jobDTO) {
		Map<String, Object> content = (Map<String, Object>) jobDTO.getContent();
		
		// TODO - Esto cambia por entrada de mapa
		if (content instanceof Task) {
			ExecutionResult resultado = jobService.run(((Task) content).getName());
			log.info("{}", resultado);
		}
		
		if (content instanceof MonthYearDTO) {
			MonthYearDTO monthYearDTO = (MonthYearDTO) content;
			ExecutionResult resultado = jobService.runImportByMonthAndYear(monthYearDTO.getYear(), monthYearDTO.getMonth());
			log.info("{}", resultado);
		}
	}

}
