package net.bounceme.chronos.suncalc.tasklet;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

	@Override
	@SneakyThrows
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		repositoryCollectionCustom.setCollectionName(collection);
		
		Map<String, Object> parameters = chunkContext.getStepContext().getJobParameters();
		Integer year = (Integer) parameters.get("year");
		
		// Si es el año actual, hasta la fecha actual
		Date currentDate = new Date();
		Integer anio = SuncalcHelper.obtenerAnio(currentDate);
		String sDesde = String.format("%d-01-01", year);
		String sHasta = (anio.equals(year)) ? dateFormat.format(currentDate) : String.format("%d-12-31", year);
		log.debug("Registros entre {} y {}", sDesde, sHasta);
		
		// Recogerlos ordenados por fecha (_id)
		List<TimeData> times = timeDataRepository.listRegistros(sDesde, sHasta, Sort.by(Sort.Direction.ASC, "_id"));
		
		/** 
		 * Valor de frontera. Si existe fecha anterior al 01-01 del año a calcular, guarda la diferencia
		 */
		Date desde = dateFormat.parse(sDesde);
		ZoneId zone = ZoneId.systemDefault();
		LocalDate lPrev = desde.toInstant().atZone(zone).toLocalDate().minusDays(1);
		String sPrev = dateFormat.format(Date.from(lPrev.atStartOfDay(zone).toInstant()));
		timeDataRepository.findById(sPrev).ifPresent(prevTimeData -> 
			timeDataRepository.findById(sDesde).ifPresent(thisTimeData -> 
				SuncalcHelper.createDifferences(thisTimeData, prevTimeData).ifPresent(d -> {
					log.debug("Diferences[{}] -> dawn: {}, sunrise: {}, culmination: {}, sunset: {}, dusk: {}",
							d.getId(), d.getDawn(), d.getSunrise(), d.getCulmination(), d.getSunset(), d.getDusk());
					differencesDataRepository.save(d);
				})
			)
		);

		for (int i = 0; i < times.size() - 1; i++) {
			TimeData nextData = times.get(i + 1);
			TimeData prevData = times.get(i);

			SuncalcHelper.createDifferences(nextData, prevData).ifPresent(d -> {
				log.debug("Diferences[{}] -> dawn: {}, sunrise: {}, culmination: {}, sunset: {}, dusk: {}",
						d.getId(), d.getDawn(), d.getSunrise(), d.getCulmination(), d.getSunset(), d.getDusk());
	
				differencesDataRepository.save(d);
			});
		}

		return RepeatStatus.FINISHED;
	}
}
