package net.bounceme.chronos.suncalc.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.DifferencesDataRepository;
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
	
	@Autowired
	private SuncalcHelper helper;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		TimeData nextData = (TimeData) chunkContext.getStepContext().getJobExecutionContext().get("NEXT_TIME_DATA");
		TimeData prevData = (TimeData) chunkContext.getStepContext().getJobExecutionContext().get("PREV_TIME_DATA");

		Differences d = helper.createDifferences(nextData, prevData);

		log.info("Diferences[{}] -> dawn: {}, sunrise: {}, culmination: {}, sunset: {}, dusk: {}",
				d.getId(), d.getDawn(), d.getSunrise(), d.getCulmination(), d.getSunset(), d.getDusk());
		
		differencesDataRepository.save(d);

		return RepeatStatus.FINISHED;
	}
}
