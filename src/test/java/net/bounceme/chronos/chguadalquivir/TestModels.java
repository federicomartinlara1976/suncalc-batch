package net.bounceme.chronos.chguadalquivir;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.model.Task;

@SpringBootTest
@Slf4j
public class TestModels {

	@Test
	public void testExecutionModel() {
		Execution execution = new Execution();
		execution.setId("2023-01-23");
		execution.setValue(1);
		execution.setExecutionTime(1L);
		assertNotNull(execution);

		assertEquals("2023-01-23", execution.getId());
		assertEquals(1, execution.getValue());
		assertEquals(1L, execution.getExecutionTime());
		log.info("{}, {}", execution.toString(), execution.hashCode());
	}
	
	@Test
	public void testTask() {
		Task task = Task.builder().name("task").build();
		assertNotNull(task);
		
		assertEquals("task", task.getName());
		
		task = new Task("task");
		assertNotNull(task);
		
		assertEquals("task", task.getName());
		
		task = new Task();
		task.setName("task");
		
		assertNotNull(task);
		
		assertEquals("task", task.getName());
	}
}
