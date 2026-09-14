package net.bounceme.chronos.suncalc.reader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
@Slf4j
public class MonthRegisterItemReader implements ItemReader<TimeData>, ItemStream {
	
	private JobExecution jobExecution;
	
	@Autowired
	@Qualifier("dateFormat")
	private SimpleDateFormat dateFormat;
	
	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;
	
	@Autowired
	private ExecutionsRepository executionsRepository;
	
	private List<TimeData> records;
	
	private Integer index = 0;
	
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}
	
	@Override
	public void open(ExecutionContext executionContext) {
		initialize();
	}

	/**
	 * 
	 */
	@SneakyThrows
	private void initialize() {
		JobParameters parameters = jobExecution.getJobParameters();
		JobParameter<?> pMonth = parameters.getParameter("month");
		JobParameter<?> pYear = parameters.getParameter("year");
		
		Assert.notNull(pMonth, "No se ha obtenido el mes");
		Assert.notNull(pYear, "No se ha obtenido el año");
		
		Integer year = (Integer) pYear.getValue();
		Integer month = (Integer) pMonth.getValue();
		
		Integer diasMes = SuncalcHelper.getDiasDelMes(month, year);
		records = new ArrayList<>();
		
		// Obtener las ejecuciones no realizadas
		for (int i = 1; i<= diasMes; i++) {
			String sDate = String.format("%d-%d-%d", year, month, i);
			if(!executionsRepository.existsById(sDate)) {
				log.info("Obteniendo para fecha {}", sDate);
				//records.add(documentProcessor.process());
			}
		}

		index = 0;
	}

	/**
	 *
	 */
	@Override
	public TimeData read() {
		TimeData nextElement = null;

		if (index < records.size()) {
			nextElement = records.get(index);
			index++;
		} else {
			index = 0;
		}

		return nextElement;
	}

}
