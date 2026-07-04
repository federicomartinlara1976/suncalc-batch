package net.bounceme.chronos.chguadalquivir;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import net.bounceme.chronos.suncalc.listener.CustomChunkListener;
import net.bounceme.chronos.suncalc.listener.ImportJobListener;
import net.bounceme.chronos.suncalc.listener.LastExecutionsListener;
import net.bounceme.chronos.suncalc.listener.StatExecutionsListener;
import net.bounceme.chronos.suncalc.listener.TimeStepListener;

@SpringBootTest
public class TestListeners {
	
	@Autowired
	private LastExecutionsListener lastExecutionsListener;
	
	@Autowired
	private ImportJobListener importJobListener;
	
	@Autowired
	private StatExecutionsListener statExecutionsListener;
	
	@Autowired
	private TimeStepListener timeStepListener;
	
	@Autowired
	private CustomChunkListener customChunkListener;

	@Test
	public void testLastExecutionsListener() {
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
		lastExecutionsListener.beforeJob(jobExecution);
		lastExecutionsListener.afterJob(jobExecution);
		
		assertTrue(true);
	}
	
	@Test
	public void testImportJobListener() {
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
		
		importJobListener.beforeJob(jobExecution);
		importJobListener.afterJob(jobExecution);
		
		assertTrue(true);
	}
	
	@Test
	public void testStatExecutionsListener() {
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
		statExecutionsListener.beforeJob(jobExecution);
		statExecutionsListener.afterJob(jobExecution);
		
		assertTrue(true);
	}
	
	@Test
	public void testTimeStepListener() {
		JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
		jobExecution.getExecutionContext().put("STEP_TIMES", new HashMap<String, Long>());
		
		StepExecution stepExecution = new StepExecution("step", jobExecution);
		timeStepListener.beforeStep(stepExecution);
		ExitStatus status = timeStepListener.afterStep(stepExecution);
		
		assertEquals(ExitStatus.COMPLETED, status);
	}
	
	@Test
	public void testCustomChunkListener() {
		ChunkContext chunkContext = createChunkContext("sampleStep");
		customChunkListener.beforeChunk(chunkContext);
		customChunkListener.afterChunk(chunkContext);
		customChunkListener.afterChunkError(chunkContext);
	}
	
	private ChunkContext createChunkContext(String nameStep) {
		JobExecution jobExecution = createJobExecution();
		
		StepContext stepContext = new StepContext(createStepExecution(nameStep, jobExecution));
		ChunkContext chunkContext = new ChunkContext(stepContext);
		return chunkContext;
	}
	
	private JobExecution createJobExecution() {
		return MetaDataInstanceFactory.createJobExecution();
	}
	
	private StepExecution createStepExecution(String name, JobExecution jobExecution) {
		return new StepExecution(name, jobExecution);
	}
}
