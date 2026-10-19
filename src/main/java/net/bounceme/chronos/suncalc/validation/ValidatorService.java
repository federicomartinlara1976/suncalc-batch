package net.bounceme.chronos.suncalc.validation;

/**
 * @author federico
 *
 */
public interface ValidatorService<Registro> {
	
	/**
	 * @param T
	 */
	void validate(Registro obj);
}
