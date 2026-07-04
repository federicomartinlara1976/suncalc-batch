package net.bounceme.chronos.suncalc.validation;

/**
 * @author federico
 *
 */
public interface ValidatorService<T> {
	
	/**
	 * @param T
	 */
	void validate(T obj);
}
