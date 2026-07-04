package net.bounceme.chronos.suncalc.processor;

import java.text.SimpleDateFormat;

import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.validation.ValidatorService;

/**
 * @author federico
 *
 */
@Component
@Slf4j
public class TimeDataValidator implements Validator<TimeData> {

	@Autowired
	@Qualifier("dateFormat")
	private SimpleDateFormat dateFormat;

	@Autowired
	private ValidatorService<TimeData> validatorService;

	@Override
	public void validate(TimeData value) {
		try {
			validatorService.validate(value);
		} catch (ConstraintViolationException e) {
			for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
				log.error(violation.getMessage());
			}
			throw new ValidationException(String.format("El time data para la fecha [%s] no se va a procesar",
					dateFormat.format(value.getFecha())));
		}
	}
}
