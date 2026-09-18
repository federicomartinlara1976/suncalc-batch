package net.bounceme.chronos.suncalc.tasklet;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
public class CalculateDifferenceTasklet implements Tasklet {
	
	@Autowired
	private DifferencesDataRepository differencesDataRepository;
	
	@Value("${application.importTimes.collection}")
	protected String collection;
	
	@Autowired
	protected TimeDataRepository timeDataRepository;
	
	@Autowired
	protected RepositoryCollectionCustom repositoryCollectionCustom;
	
	@Autowired
	protected SimpleDateFormat dateFormat;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		repositoryCollectionCustom.setCollectionName(collection);
		
		TimeData nextData = (TimeData) chunkContext.getStepContext().getJobExecutionContext().get("NEXT_TIME_DATA");
		
		Date fecha = nextData.getFecha();
		
		// prevData será la fecha anterior a nextData
		ZoneId zone = ZoneId.systemDefault();
		LocalDate lPrevDate = fecha.toInstant().atZone(zone).toLocalDate().minusDays(1);
		String idPrevDate = dateFormat.format(Date.from(lPrevDate.atStartOfDay(zone).toInstant()));
		
		timeDataRepository.findById(idPrevDate).ifPresent(prevData -> {
			Differences d = SuncalcHelper.createDifferences(nextData, prevData);

			log.info("Diferences[{}] -> dawn: {}, sunrise: {}, culmination: {}, sunset: {}, dusk: {}",
					d.getId(), d.getDawn(), d.getSunrise(), d.getCulmination(), d.getSunset(), d.getDusk());
			
			differencesDataRepository.save(d);
		});

		

		return RepeatStatus.FINISHED;
	}
}
