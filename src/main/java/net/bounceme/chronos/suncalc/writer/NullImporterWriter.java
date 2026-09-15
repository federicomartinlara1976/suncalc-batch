package net.bounceme.chronos.suncalc.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;

@Component
@Slf4j
public class NullImporterWriter implements ItemWriter<TimeData> {
	
    @Override
    public synchronized void write(Chunk<? extends TimeData> items) throws Exception {
        items.forEach(item ->
            log.info("Writing {}", item));
    }

}
