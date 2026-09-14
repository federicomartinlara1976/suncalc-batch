package net.bounceme.chronos.suncalc.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.model.Task;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.MonthYearDTO;
import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.services.JobService;

@Component
@Slf4j
public class JobListener {
	
	@Autowired
	private JobService jobService;
	
	@EventListener
	public void executeJob(JobDTO<?> jobDTO) {
		Object content = jobDTO.getContent();
		
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
