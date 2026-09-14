package net.bounceme.chronos.suncalc.reader;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;

import net.bounceme.chronos.suncalc.model.TimeData;

public abstract class AbstractItemReader implements ItemReader<TimeData>, ItemStream {
	
	protected JobExecution jobExecution;
	
	protected List<TimeData> records = new ArrayList<>();
	
	private Integer index = 0;
	
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}
	
	@Override
	public void open(ExecutionContext executionContext) {
		initialize();
	}

	/**
	 * 
	 */
	protected abstract void initialize();

	/**
	 *
	 */
	@Override
	public TimeData read() {
		TimeData nextElement = null;

		if (index < records.size()) {
			nextElement = records.get(index);
			index++;
		} else {
			index = 0;
		}

		return nextElement;
	}

}
