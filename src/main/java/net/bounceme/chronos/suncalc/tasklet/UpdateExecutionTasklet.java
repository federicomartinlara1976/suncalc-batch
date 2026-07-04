package net.bounceme.chronos.suncalc.tasklet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;

/**
 * @author fxm105
 *
 */
@Component
@Slf4j
public class UpdateExecutionTasklet implements Tasklet {
	
	@Autowired
	private ExecutionsRepository executionsRepository;
	
	@Autowired
	private SimpleDateFormat dateFormat;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		Date date = new Date();
		
		List<Execution> executions = executionsRepository.findByDate(dateFormat.format(date));
		Execution execution = null;
		
		if (CollectionUtils.isEmpty(executions)) {
        	execution = new Execution();
        	execution.setId(dateFormat.format(date));
        	execution.setValue(1);
        	
        	Long duration = getDuration(chunkContext, "importStep");
        	execution.setExecutionTime(duration);
        }
		else {
			execution = executions.get(0);
			Integer value = execution.getValue() + 1;
			execution.setValue(value);
		}
		
		executionsRepository.save(execution);
		
		log.info("Execution count updated");
		
		return RepeatStatus.FINISHED;
	}

	/**
	 * @param chunkContext
	 * @param step
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Long getDuration(ChunkContext chunkContext, String step) {
		Map<String, Long> stepTimes = (Map<String, Long>) chunkContext.getStepContext().getJobExecutionContext().get("STEP_TIMES");
		return stepTimes.get(step);
	}
}
