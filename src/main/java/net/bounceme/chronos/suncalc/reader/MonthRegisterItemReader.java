package net.bounceme.chronos.suncalc.reader;

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
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
@Slf4j
public class MonthRegisterItemReader extends AbstractItemReader {

	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;

	@Autowired
	private ExecutionsRepository executionsRepository;

	/**
	 * 
	 */
	@SneakyThrows
	protected void initialize() {
		JobParameters parameters = jobExecution.getJobParameters();
		JobParameter<?> pMonth = parameters.getParameter("month");
		JobParameter<?> pYear = parameters.getParameter("year");

		Assert.notNull(pMonth, "No se ha obtenido el mes");
		Assert.notNull(pYear, "No se ha obtenido el año");

		Integer year = (Integer) pYear.getValue();
		Integer month = (Integer) pMonth.getValue();

		// Si es el mes y año actual, sólo hasta el día del mes actual
		Date currentDate = new Date();
		Integer anio = SuncalcHelper.obtenerAnio(currentDate);
		Integer mes = SuncalcHelper.obtenerMes(currentDate);
		Integer diasMes = (mes.equals(month) && anio.equals(year)) ? SuncalcHelper.obtenerDia(currentDate)
				: SuncalcHelper.getDiasDelMes(month, year);

		records = new ArrayList<>();

		for (int i = 1; i <= diasMes; i++) {
			String sDate = String.format("%d-%s-%s", year, SuncalcHelper.normalize(month), SuncalcHelper.normalize(i));

			if (!executionsRepository.existsById(sDate)) {
				log.debug("Obteniendo para fecha {}", sDate);
				records.add(documentProcessor.process(sDate));

				// Por cada proceso, registrar la ejecución
				executionsRepository.save(Execution.builder().id(sDate).value(1).build());
			}
		}
	}
}
