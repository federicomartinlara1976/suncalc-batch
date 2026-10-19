package net.bounceme.chronos.suncalc.services.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.repository.MoonDataRepository;
import net.bounceme.chronos.suncalc.services.CalcService;
import net.bounceme.chronos.suncalc.services.EfemeridesService;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;
import net.bounceme.chronos.utils.calc.converters.Converter;

@Service
@Validated
@Slf4j
public class EfemeridesServiceImpl implements EfemeridesService {
	
	private static final String[] EQUINOXES = {"march_equinox", "september_equinox"};
	private static final String[] SOLSTICES = {"june_solstice", "december_solstice"};
	
	@Value("${application.importTimes.timeZone}")
	private String timeZone;
	
	@Value("${application.importTimes.coords.lat}")
	private Float latitude;
	
	@Value("${application.importTimes.coords.lng}")
	private Float longitude;
	
	@Autowired
	private SimpleDateFormat dateFormat;
	
	@Autowired
	private CalcService calcService;
	
	@Autowired
	private MoonDataRepository moonDataRepository;
	
	@Autowired
	private Converter<BigDecimal[], Date> dateConverter;
	
	@SuppressWarnings("rawtypes")
	@Autowired
	@Qualifier("moonDataDocumentProcessor")
	private DocumentProcessor documentProcessor;
	
	@Override
	@SneakyThrows
	public List<DataSolsticeEquinoxDTO> calculateSolsticesEquinoxes(Integer year) {
		Assert.notNull(year, "El año no puede ser nulo");
		
		return Stream.concat(
                Arrays.stream(SOLSTICES),
                Arrays.stream(EQUINOXES)
            )
            .map(evento -> extractFor(year, evento))
            .sorted((data1, data2) -> data1.getUtcDate().compareTo(data2.getUtcDate()))
            .toList();
	}
	
	private DataSolsticeEquinoxDTO extractFor(Integer year, String item) {
		final String CMD_TEMPLATE = "[JDE, utc, local, off] = equinoccio_solsticio(%d, '%s', '%s')";
		String cmd = String.format(CMD_TEMPLATE, year, item, timeZone);
		
		calcService.execute(cmd);
		
		// Retrieve the data and parse dates
		Date utcDate = dateConverter.apply(calcService.getArray("utc"));
 		Date localDate = dateConverter.apply(calcService.getArray("local"));
 		
 		return DataSolsticeEquinoxDTO.builder().name(item).utcDate(utcDate).localDate(localDate).build();
	}

	@Override
	public FaseLunarDTO calculateFaseLunar(String date) {
		
		return moonDataRepository.findById(date).map(f -> toFaseLunarDTO(f, date)).orElseGet(() -> {
			Registro<?> registro = documentProcessor.process();
			return (FaseLunarDTO) registro.getContent();
		});
	}
	
	private FaseLunarDTO toFaseLunarDTO(MoonData f, String date) {
	    return FaseLunarDTO.builder()
	    	.date(parseDate(date))
	    	.weekday(f.getWeekday())
	    	.phaseName(f.getPhaseName())
	    	.phaseEmoji(f.getPhaseEmoji())
		    .illuminationPercent(f.getIlluminationPercent())
		    .moonAgeDays(f.getMoonAgeDays())
		    .distanceKm(f.getDistanceKm())
		    .principalPhase(f.getPrincipalPhase())
		    .principalPhaseTime(f.getPrincipalPhaseTime())
		    .moonrise(f.getMoonrise())
		    .moonset(f.getMoonset())
		    .build();
	}

	private Date parseDate(String date) {
	    try {
	        return dateFormat.parse(date);
	    } catch (ParseException e) {
	        log.error("Error al parsear la fecha {}: {}", date, e.getMessage());
	        return null;
	    }
	}
}
