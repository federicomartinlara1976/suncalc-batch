package net.bounceme.chronos.suncalc.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Service
public class SuncalcServiceImpl implements SuncalcService {
	
	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;

	@Override
	public TimeData getCurrentTimeData() {
		return documentProcessor.process();
	}

}
