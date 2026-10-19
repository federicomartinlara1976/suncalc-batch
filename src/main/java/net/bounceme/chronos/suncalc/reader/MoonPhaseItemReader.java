package net.bounceme.chronos.suncalc.reader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
public class MoonPhaseItemReader extends AbstractItemReader {
	
	@Autowired
	@Qualifier("moonPhaseDocumentProcessor")
	private DocumentProcessor<MoonData> documentProcessor;

	/**
	 * 
	 */
	@SneakyThrows
	protected void initialize() {
		records.add(documentProcessor.process());
	}
}
