package net.bounceme.chronos.chguadalquivir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.flow.IsExecutedDecider;
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.tasklet.UpdateExecutionTasklet;

@SpringBootTest
@Slf4j
public class TestTasklets {
	
	private static final FlowExecutionStatus NO_EXECUTED = new FlowExecutionStatus("NO_EXECUTED");
	private static final FlowExecutionStatus EXECUTED = new FlowExecutionStatus("EXECUTED");
		
	@Autowired
	private UpdateExecutionTasklet updateExecutionTasklet; 
	
	@Autowired
	private IsExecutedDecider isExecutedDecider;
	
	@MockBean
	private ExecutionsRepository executionsRepository;
	
	@Value("${application.importJob.url}")
	private String url;
	
	@Test
	public void testUpdateExecutionTasklet() {
		try {
			ChunkContext chunkContext = createChunkContext("countExecutionStep", buildStepTimes());
			
			Mockito.doReturn(Collections.emptyList()).when(executionsRepository).findByDate(Mockito.isA(String.class));
			Mockito.doReturn(new Execution()).when(executionsRepository).save(Mockito.isA(Execution.class));
			
			RepeatStatus status = updateExecutionTasklet.execute(null, chunkContext);
			assertEquals(RepeatStatus.FINISHED, status);
			
			Mockito.doReturn(buildExecution()).when(executionsRepository).findByDate(Mockito.isA(String.class));
			status = updateExecutionTasklet.execute(null, chunkContext);
			assertEquals(RepeatStatus.FINISHED, status);
		} catch (Exception e) {
			log.error(e.getMessage());
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testIsExecutedDecider() {
		try {
			Mockito.doReturn(Collections.emptyList()).when(executionsRepository).findByDate(Mockito.isA(String.class));
			JobExecution jobExecution = createJobExecution();
			StepExecution stepExecution = createStepExecution("stepFlowDecider", jobExecution);
			
			FlowExecutionStatus status = isExecutedDecider.decide(jobExecution, stepExecution);
			assertEquals(NO_EXECUTED, status);
			
			Mockito.doReturn(buildExecution()).when(executionsRepository).findByDate(Mockito.isA(String.class));
			status = isExecutedDecider.decide(jobExecution, stepExecution);
			assertEquals(EXECUTED, status);
		} catch (Exception e) {
			log.error(e.getMessage());
			Assert.fail(e.getMessage());
		}
	}
	
	private ChunkContext createChunkContext(String name, List<Execution> executions) {
		JobExecution jobExecution = createJobExecution();
		jobExecution.getExecutionContext().put("EXECUTIONS", executions);
		
		StepContext stepContext = new StepContext(new StepExecution(name, jobExecution));
		ChunkContext chunkContext = new ChunkContext(stepContext);
		return chunkContext;
	}
	
	private ChunkContext createChunkContext(String name, Map<String, Long> stepTimes) {
		JobExecution jobExecution = createJobExecution();
		jobExecution.getExecutionContext().put("STEP_TIMES", stepTimes);
		
		StepContext stepContext = new StepContext(createStepExecution(name, jobExecution));
		ChunkContext chunkContext = new ChunkContext(stepContext);
		return chunkContext;
	}
	
	private JobExecution createJobExecution() {
		return MetaDataInstanceFactory.createJobExecution();
	}
	
	private StepExecution createStepExecution(String name, JobExecution jobExecution) {
		return new StepExecution(name, jobExecution);
	}
	
	private List<Execution> buildExecutions() {
		List<Execution> executions = new ArrayList<>();
		
		Execution execution = Execution.builder().id("2023-01-23").value(1).executionTime(1896L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-24").value(1).executionTime(1923L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-25").value(1).executionTime(1642L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-26").value(1).executionTime(1828L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-27").value(1).executionTime(1805L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-28").value(1).executionTime(1665L).build();
		executions.add(execution);
		
		return executions;
	}
	
	private List<Execution> buildExecution() {
		List<Execution> executions = new ArrayList<>();
		
		Execution execution = Execution.builder().id("2023-01-23").value(1).executionTime(1896L).build();
		executions.add(execution);
		
		return executions;
	}
	
	private Map<String, Long> buildStepTimes() {
		Map<String, Long> map = new HashMap<>();
		
		map.put("importStep", 1L);
		
		return map;
	}
}
