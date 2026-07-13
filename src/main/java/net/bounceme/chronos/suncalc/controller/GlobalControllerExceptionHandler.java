package net.bounceme.chronos.suncalc.controller;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
class GlobalControllerExceptionHandler {

	@ExceptionHandler(DataAccessException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<Map<String, String>> handleException(DataAccessException ex) {
		log.error("ERROR: {}", ex.getMessage());

		Map<String, String> errors = new HashMap<>();
		errors.put("mensaje", ex.getMostSpecificCause().getMessage());

		return ResponseEntity.internalServerError().body(errors);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		return ResponseEntity.badRequest().body(errors);
	}
	
	@ExceptionHandler(ParseException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<Map<String, String>> handleParseException(ParseException ex) {
		Map<String, String> errors = new HashMap<>();
		errors.put("mensaje", ex.getMessage());

		return ResponseEntity.badRequest().body(errors);
	}
	
	@ResponseBody
	@ExceptionHandler(HandlerMethodValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleValidationException(HandlerMethodValidationException ex) {
        // Registra la traza completa del error
        return new ResponseEntity<>("Error de validación: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
	}
}
