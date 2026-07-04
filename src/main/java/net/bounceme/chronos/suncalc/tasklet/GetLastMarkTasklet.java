package net.bounceme.chronos.suncalc.tasklet;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

/**
 * @author fxm105
 *
 */
@Component
public class GetLastMarkTasklet implements Tasklet {
	
	@Value("${application.importTimes.collection}")
	private String collection;
	
	@Autowired
	private TimeDataRepository timeDataRepository;
	
	@Autowired
	private RepositoryCollectionCustom repositoryCollectionCustom;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		repositoryCollectionCustom.setCollectionName(collection);
		
		List<TimeData> list = timeDataRepository.findAll();
		
		TimeData lastTimeData = list.get(list.size()-1);
		
		chunkContext
			.getStepContext()
			.getStepExecution()
			.getJobExecution()
			.getExecutionContext()
			.put("PREV_TIME_DATA", lastTimeData);
		
		return RepeatStatus.FINISHED;
	}
}
