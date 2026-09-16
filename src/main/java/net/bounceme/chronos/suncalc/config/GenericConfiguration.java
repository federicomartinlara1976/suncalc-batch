package net.bounceme.chronos.suncalc.config;

import java.text.SimpleDateFormat;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.validation.ValidatorService;
import net.bounceme.chronos.suncalc.validation.impl.ValidatorServiceImpl;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@ComponentScan(basePackages = {"net.bounceme.chronos.notifications"})
public class GenericConfiguration {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	
	private static final String TIME_FORMAT = "hh:mm:ss";
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss";

	@Bean
	@Scope("prototype")
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
	
	@Bean(name = "dateFormat")
	SimpleDateFormat dateFormat() {
		return new SimpleDateFormat(DATE_FORMAT);
	}
	
	@Bean(name = "timeFormat")
	SimpleDateFormat timeFormat() {
		return new SimpleDateFormat(TIME_FORMAT);
	}
	
	@Bean(name = "dateTimeFormat")
	SimpleDateFormat dateTimeFormat() {
		return new SimpleDateFormat(DATE_TIME_FORMAT);
	}
	
	@Bean
	@Scope("prototype")
	ValidatorService<TimeData> timeDataValidatorService() {
		return new ValidatorServiceImpl<>();
	}
}
