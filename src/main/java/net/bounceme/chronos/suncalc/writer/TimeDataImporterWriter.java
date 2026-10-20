package net.bounceme.chronos.suncalc.writer;

import java.util.Objects;

import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.model.TimeData;

@Component
@Slf4j
public class TimeDataImporterWriter extends ImporterWriter {
	
    @Override
    @SneakyThrows
    protected void doWrite(Chunk<? extends Registro<TimeData>> items) {
        items.forEach(item -> {
        	TimeData timeData = item.getContent();
            repositoryCollectionCustom.setCollectionName(collection);
            
            timeDataRepository.findById(dateFormat.format(timeData.getFecha())).ifPresentOrElse(t ->
            	log.warn("Time data {} already registered", t)
            , () -> {
            	log.debug("Writing {}", item);
            	// Set id if null
            	if (Objects.isNull(timeData.getId())) {
            		timeData.setId(dateFormat.format(timeData.getFecha()));
            	}
            
            	timeDataRepository.save(timeData);
            	jobExecution.getExecutionContext().put("NEXT_TIME_DATA", timeData);
            });
        });
    }

}
