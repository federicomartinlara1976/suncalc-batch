package net.bounceme.chronos.suncalc.writer;

import java.text.SimpleDateFormat;
import java.util.Objects;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.repository.MoonDataRepository;

@Component
@Slf4j
public class MoonPhaseWriter implements ItemWriter<MoonData> {
	
	protected JobExecution jobExecution;
	
	@Autowired
	protected MoonDataRepository moonDataRepository;
	
	@Autowired
	protected SimpleDateFormat dateFormat;

	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		jobExecution = stepExecution.getJobExecution();
	}
	
    @Override
    @SneakyThrows
    public synchronized void write(Chunk<? extends MoonData> items) {
    	items.forEach(item -> 
            moonDataRepository.findById(dateFormat.format(item.getFecha())).ifPresentOrElse(timeData ->
            	log.warn("Moon data {} already registered", timeData)
            , () -> {
            	log.debug("Writing {}", item);
            	// Set id if null
            	if (Objects.isNull(item.getId())) {
            		item.setId(dateFormat.format(item.getFecha()));
            	}
            
            	moonDataRepository.save(item);
            	jobExecution.getExecutionContext().put("NEXT_TIME_DATA", item);
            })
        );
    }

}
