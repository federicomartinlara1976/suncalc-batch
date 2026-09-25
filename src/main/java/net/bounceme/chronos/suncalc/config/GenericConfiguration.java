package net.bounceme.chronos.suncalc.config;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
import net.bounceme.chronos.utils.calc.converters.Converter;

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
	
	@Bean
	@Scope("prototype")
	Converter<BigDecimal[], Date> dateConverter() {
		Converter<BigDecimal[], Date> converter = a -> {
			Integer year = a[0].intValue();
			Integer month = a[1].intValue();
			Integer day = a[2].intValue();
			Integer hour = a[3].intValue();
			Integer minute = a[4].intValue();
			Float second = a[5].floatValue();
			
			Integer iSecond = second.intValue();
			
			Calendar cal = Calendar.getInstance();
		    cal.set(Calendar.YEAR, year);
		    cal.set(Calendar.MONTH, month - 1); // Calendar es 0-based (enero = 0)
		    cal.set(Calendar.DAY_OF_MONTH, day);
		    cal.set(Calendar.HOUR_OF_DAY, hour);
		    cal.set(Calendar.MINUTE, minute);
		    cal.set(Calendar.SECOND, second.intValue());
		    cal.set(Calendar.MILLISECOND, (int) ((second - iSecond) * 1000));

		    return cal.getTime();
		};
		
		return converter;
	}
}
