package net.bounceme.chronos.suncalc.writer;

import java.util.Objects;

import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;

@Component
@Slf4j
public class UpdateTimeDataImporterWriter extends ImporterWriter {
	
    @Override
    @SneakyThrows
    protected void doWrite(Chunk<? extends TimeData> items) {
        items.forEach(item -> {
            repositoryCollectionCustom.setCollectionName(collection);
            
            log.debug("Writing {}", item);
        	// Set id if null
        	if (Objects.isNull(item.getId())) {
        		item.setId(dateFormat.format(item.getFecha()));
        	}
        
        	timeDataRepository.save(item);
        });
    }

}
