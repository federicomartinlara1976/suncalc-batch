package net.bounceme.chronos.suncalc.validation.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import net.bounceme.chronos.suncalc.validation.Validador;

public class FechaImpl extends Validador<Fecha, String> {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@Override
	protected boolean esInvalido(String value) {
		try {
			(new SimpleDateFormat(DATE_FORMAT)).parse(value);
			return false;
		} catch (ParseException e) {
			return true;
		}
	}
}
