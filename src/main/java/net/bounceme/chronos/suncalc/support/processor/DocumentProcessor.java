package net.bounceme.chronos.suncalc.support.processor;

import net.bounceme.chronos.suncalc.model.TimeData;

public interface DocumentProcessor {

	void setUrl(String url);
	
	TimeData process();
	
	TimeData process(String date);

}