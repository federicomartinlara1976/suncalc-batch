package net.bounceme.chronos.suncalc.support.processor;

import net.bounceme.chronos.suncalc.model.Registro;

public interface DocumentProcessor<T> {

	Registro<T> process();
	
	Registro<T> process(String date);

}