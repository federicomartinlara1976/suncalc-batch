package net.bounceme.chronos.suncalc.tasklet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.DifferencesDataRepository;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

/**
 * @author fxm105
 *
 */
@Component
@Slf4j
public class CalculateDifferencesExecutionTasklet implements Tasklet {

	private JobExecution jobExecution;
	
	@Value("${application.importTimes.collection}")
	private String collection;

	@Autowired
	private TimeDataRepository timeDataRepository;

	@Autowired
	private DifferencesDataRepository differencesDataRepository;

	@Autowired
	private RepositoryCollectionCustom repositoryCollectionCustom;
	
	@Autowired
	private SimpleDateFormat dateFormat;
	
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}

	@Override
	@SneakyThrows
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		repositoryCollectionCustom.setCollectionName(collection);
		
		JobParameters parameters = jobExecution.getJobParameters();
		JobParameter<?> pYear = parameters.getParameter("year");
		
		Assert.notNull(pYear, "No se ha obtenido el año");
		Integer year = (Integer) pYear.getValue();
		
		// Si es el año actual, hasta la fecha actual
		Date currentDate = new Date();
		Integer anio = SuncalcHelper.obtenerAnio(currentDate);
		String desde = String.format("%d-01-01", year);
		String hasta = (anio.equals(year)) ? dateFormat.format(currentDate) : String.format("%d-12-31", year);
		log.info("Registros entre {} y {}", desde, hasta);
		
		// Recogerlos ordenados por fecha (_id)
		List<TimeData> times = timeDataRepository.listRegistros(desde, hasta, Sort.by(Sort.Direction.ASC, "_id"));

		for (int i = 0; i < times.size() - 1; i++) {
			TimeData nextData = times.get(i + 1);
			TimeData prevData = times.get(i);

			Differences d = SuncalcHelper.createDifferences(nextData, prevData);

			if (!Objects.isNull(d.getId())) {
				log.info("Diferences[{}] -> dawn: {}, sunrise: {}, culmination: {}, sunset: {}, dusk: {}",
						d.getId(), d.getDawn(), d.getSunrise(), d.getCulmination(), d.getSunset(), d.getDusk());
	
				differencesDataRepository.save(d);
			}
		}

		return RepeatStatus.FINISHED;
	}
}
