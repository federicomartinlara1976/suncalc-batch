package net.bounceme.chronos.suncalc.support.processor;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.clients.SunriseSunsetRest;
import net.bounceme.chronos.suncalc.model.SunriseSunsetResponse;
import net.bounceme.chronos.suncalc.model.TimeData;

@Component("feignDocumentProcessor")
@Slf4j
public class FeignDocumentProcessor implements DocumentProcessor {
	
	private static final String TODAY = "today";
	
	@Autowired
	private SunriseSunsetRest sunriseSunsetRest;
	
	@Autowired
	@Qualifier("dateFormat")
	private SimpleDateFormat dateFormat;
	
	@Autowired
	@Qualifier("dateTimeFormat")
	private SimpleDateFormat dateTimeFormat;
	
	@Value("${application.importTimes.coords.lat}")
	private Float lat;
	
	@Value("${application.importTimes.coords.lng}")
	private Float lng;
	
	@Setter
	private String url;

	@Override
	public TimeData process() {
		return obtainData(TODAY);
	}

	@SneakyThrows
	private TimeData obtainData(String sDate) {
		TimeData timeData = new TimeData();
		
		Boolean status = Boolean.FALSE;
		// La fecha es la que viene a partir de sDate, excepto si es "today"
		Date date = (!TODAY.equals(sDate)) ? dateFormat.parse(sDate) : new Date();
		timeData.setFecha(date);
		
		SunriseSunsetResponse response = sunriseSunsetRest.detalle(
	            lat, 
	            lng, 
	            sDate, 
	            "Europe/Madrid", 
	            0, 
	            null
	        );
		
		if ("OK".equals(response.getStatus())) {
			OffsetDateTime oDawn = OffsetDateTime.parse(response.getResults().getCivil_twilight_begin());
			OffsetDateTime oSunrise = OffsetDateTime.parse(response.getResults().getSunrise());
			OffsetDateTime oCulmination = OffsetDateTime.parse(response.getResults().getSolar_noon());
			OffsetDateTime oSunset = OffsetDateTime.parse(response.getResults().getSunset());
			OffsetDateTime oDusk = OffsetDateTime.parse(response.getResults().getCivil_twilight_end());
			
			timeData.setDawn(Date.from(oDawn.toInstant()));
			timeData.setSunrise(Date.from(oSunrise.toInstant()));
			timeData.setCulmination(Date.from(oCulmination.toInstant()));
			timeData.setSunset(Date.from(oSunset.toInstant()));
			timeData.setDusk(Date.from(oDusk.toInstant()));
			
			status = Boolean.TRUE;
			log.debug("Returned data:\n \tdawn:{},\n\tsunrise: {},\n\tculmination: {},\n\tsunset: {},\n\tdusk: {} ",
					timeData.getDawn(), timeData.getSunrise(), timeData.getCulmination(), timeData.getSunset(), timeData.getDusk());
		}
		
		timeData.setStatus(status);
			
		return timeData;
	}

	@Override
	public TimeData process(String date) {
		return obtainData(date);
	}
}
