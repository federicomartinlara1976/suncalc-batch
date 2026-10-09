package net.bounceme.chronos.suncalc.services.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.services.AstronomiaService;
import net.bounceme.chronos.suncalc.services.CalcService;
import net.bounceme.chronos.utils.calc.converters.Converter;

@Service
@Validated
public class AstronomiaServiceImpl implements AstronomiaService {
	
	private static final String[] EQUINOXES = {"march_equinox", "september_equinox"};
	private static final String[] SOLSTICES = {"june_solstice", "december_solstice"};
	private static final String HOUR_ZONE = "Europe/Madrid";
	
	@Autowired
	private CalcService calcService;
	
	@Autowired
	private SimpleDateFormat dateFormat;
	
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
		String cmd = String.format(CMD_TEMPLATE, year, item, HOUR_ZONE);
		
		calcService.execute(cmd);
		
		// Retrieve the data and parse dates
		Date utcDate = dateConverter.apply(calcService.getArray("utc"));
 		Date localDate = dateConverter.apply(calcService.getArray("local"));
 		
 		return DataSolsticeEquinoxDTO.builder().name(item).utcDate(utcDate).localDate(localDate).build();
	}

	@Override
	@SneakyThrows(ParseException.class)
	public FaseLunarDTO calculateFaseLunar(String date) {
		final String CMD_TEMPLATE = "[edad, fase, iluminacion] = fase_lunar(%s)";
		String cmd = String.format(CMD_TEMPLATE, date);
		
		calcService.execute(cmd);
		
		return FaseLunarDTO.builder()
				.date(dateFormat.parse(date))
				.edad(calcService.getScalar("edad").floatValue())
				.fase(calcService.getString("fase"))
				.iluminacion(calcService.getScalar("iluminacion").floatValue())
				.build();
	}
}
