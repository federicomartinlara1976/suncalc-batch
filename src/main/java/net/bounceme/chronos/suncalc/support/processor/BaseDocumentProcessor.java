package net.bounceme.chronos.suncalc.support.processor;

import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import lombok.Setter;

public abstract class BaseDocumentProcessor implements DocumentProcessor {

	@Autowired
	@Qualifier("dateFormat")
	protected SimpleDateFormat dateFormat;
	
	@Autowired
	@Qualifier("dateTimeFormat")
	protected SimpleDateFormat dateTimeFormat;
	
	@Value("${application.importTimes.timeZone}")
	protected String timeZone;
	
	@Value("${application.importTimes.coords.lat}")
	protected Float latitude;
	
	@Value("${application.importTimes.coords.lng}")
	protected Float longitude;
	
	@Setter
	protected String url;
}
