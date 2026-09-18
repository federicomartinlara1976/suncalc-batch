package net.bounceme.chronos.suncalc.writer;

import java.text.SimpleDateFormat;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

@Component
@Slf4j
public abstract class ImporterWriter implements ItemWriter<TimeData> {
	
	protected JobExecution jobExecution;
	
	@Value("${application.importTimes.collection}")
	protected String collection;
	
	@Autowired
	protected TimeDataRepository timeDataRepository;
	
	@Autowired
	protected RepositoryCollectionCustom repositoryCollectionCustom;
	
	@Autowired
	protected SimpleDateFormat dateFormat;

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}
	
	protected abstract void doWrite(Chunk<? extends TimeData> items);
	
    @Override
    @SneakyThrows
    public synchronized void write(Chunk<? extends TimeData> items) {
        doWrite(items);
    }

}
