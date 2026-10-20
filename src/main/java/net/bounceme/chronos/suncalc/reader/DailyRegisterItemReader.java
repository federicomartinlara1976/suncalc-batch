package net.bounceme.chronos.suncalc.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
public class DailyRegisterItemReader extends AbstractItemReader {
	
	@Autowired
	@Qualifier("sunDataDocumentProcessor")
	private DocumentProcessor<TimeData> documentProcessor;

	/**
	 * 
	 */
	@SneakyThrows
	protected void initialize() {
		records.add(documentProcessor.process());
	}
}
