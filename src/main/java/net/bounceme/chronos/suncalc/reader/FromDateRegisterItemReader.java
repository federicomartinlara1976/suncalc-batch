package net.bounceme.chronos.suncalc.reader;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
@Slf4j
public class FromDateRegisterItemReader extends AbstractItemReader {

	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;

	@Autowired
	private ExecutionsRepository executionsRepository;
	
	@Autowired
	private SimpleDateFormat dateFormat;

	/**
	 * 
	 */
	@SneakyThrows
	protected void initialize() {
		JobParameters parameters = jobExecution.getJobParameters();
		JobParameter<?> pDate = parameters.getParameter("date");

		Assert.notNull(pDate, "No se ha obtenido la fecha");

		String date = (String) pDate.getValue();

		records = new ArrayList<>();
		
		Date currentDate = new Date();
		Date fromDate = dateFormat.parse(date);
		
		ZoneId zone = ZoneId.systemDefault();
		LocalDate start = fromDate.toInstant().atZone(zone).toLocalDate();
		LocalDate end = currentDate.toInstant().atZone(zone).toLocalDate();

		for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
			String sDate = dateFormat.format(Date.from(d.atStartOfDay(zone).toInstant()));

			log.info("Obteniendo para fecha {}", sDate);
			//records.add(documentProcessor.process(sDate));

			// Por cada proceso, registrar la ejecución
			//executionsRepository.save(Execution.builder().id(sDate).value(1).build());
		}
	}
}
