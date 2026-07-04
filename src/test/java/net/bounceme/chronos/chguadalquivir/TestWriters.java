package net.bounceme.chronos.chguadalquivir;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import net.bounceme.chronos.suncalc.model.Execution;

@SpringBootTest
public class TestWriters {
	
	@Test
	public void testStatsExecutionWriter() throws Exception {
		List<Execution> executions = new ArrayList<>();
		StepExecution stepExecution = createStepExecution("statExecutionsStep", executions);
		
		assertTrue(true);
	}
	
	private StepExecution createStepExecution(String name, List<Execution> executions) {
		JobExecution jobExecution = createJobExecution();
		jobExecution.getExecutionContext().put("EXECUTIONS", executions);
		
		return new StepExecution(name, jobExecution);
	}
	
	private JobExecution createJobExecution() {
		return MetaDataInstanceFactory.createJobExecution();
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
}
