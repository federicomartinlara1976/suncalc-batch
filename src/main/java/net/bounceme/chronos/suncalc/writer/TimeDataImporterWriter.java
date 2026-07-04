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

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

@Component
@Slf4j
public class TimeDataImporterWriter implements ItemWriter<TimeData> {
	
	private JobExecution jobExecution;
	
	@Value("${application.importTimes.collection}")
	private String collection;
	
	@Autowired
	private TimeDataRepository timeDataRepository;
	
	@Autowired
	private RepositoryCollectionCustom repositoryCollectionCustom;
	
	@Autowired
	private SimpleDateFormat dateFormat;

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}
	
    @Override
    public synchronized void write(Chunk<? extends TimeData> items) throws Exception {
        items.forEach(item -> {
            repositoryCollectionCustom.setCollectionName(collection);
            
            timeDataRepository.findById(dateFormat.format(item.getFecha())).ifPresentOrElse(timeData ->
            	log.info("Time data {} already registered", timeData.toString())
            , () -> {
            	// Set id
            	item.setId(dateFormat.format(item.getFecha()));
            
            	timeDataRepository.save(item);
            	jobExecution.getExecutionContext().put("NEXT_TIME_DATA", item);
            });
        });
    }

}
