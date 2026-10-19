package net.bounceme.chronos.suncalc.support.processor;

import net.bounceme.chronos.suncalc.model.Registro;

public interface DocumentProcessor {

	void setUrl(String url);
	
	Registro<?> process();
	
	Registro<?> process(String date);

}