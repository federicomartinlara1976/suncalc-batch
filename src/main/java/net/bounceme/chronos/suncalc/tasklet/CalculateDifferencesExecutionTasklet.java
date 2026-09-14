package net.bounceme.chronos.suncalc.tasklet;

import java.util.List;
import java.util.Objects;

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
public class CalculateDifferencesExecutionTasklet implements Tasklet {

	@Value("${application.importTimes.collection}")
	private String collection;

	@Autowired
	private TimeDataRepository timeDataRepository;

	@Autowired
	private DifferencesDataRepository differencesDataRepository;

	@Autowired
	private RepositoryCollectionCustom repositoryCollectionCustom;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		repositoryCollectionCustom.setCollectionName(collection);
		List<TimeData> times = timeDataRepository.findAll();

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
