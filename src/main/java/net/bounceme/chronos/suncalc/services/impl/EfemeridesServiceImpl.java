package net.bounceme.chronos.suncalc.services.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.clients.FaseLunarRest;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.dto.InputFaseLunarDTO;
import net.bounceme.chronos.suncalc.services.CalcService;
import net.bounceme.chronos.suncalc.services.EfemeridesService;
import net.bounceme.chronos.utils.calc.converters.Converter;

@Service
@Validated
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
	private CalcService calcService;
	
	@Autowired
	private FaseLunarRest faseLunarRest;
	
	@Autowired
	private Converter<BigDecimal[], Date> dateConverter;
	
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
		InputFaseLunarDTO inputFaseLunarDTO = InputFaseLunarDTO.builder()
				.startDate(date)
				.latitude(latitude)
				.longitude(longitude)
				.days(1)
				.timezone(timeZone)
				.build();
		
		return faseLunarRest.detalle(inputFaseLunarDTO).get(0);
	}
}
