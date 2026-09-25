package net.bounceme.chronos.suncalc.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.services.AstronomiaService;
import net.bounceme.chronos.suncalc.services.CalcService;
import net.bounceme.chronos.utils.calc.converters.Converter;

@Service
public class AstronomiaServiceImpl implements AstronomiaService {
	
	private static final String[] EQUINOXES = {"march_equinox", "september_equinox"};
	private static final String[] SOLSTICES = {"june_solstice", "december_solstice"};
	private static final String HOUR_ZONE = "Europe/Madrid";
	
	@Autowired
	private CalcService calcService;
	
	@Autowired
	private Converter<BigDecimal[], Date> dateConverter;
	
	@Override
	public List<DataSolsticeEquinoxDTO> calculateSolsticesEquinoxes(Integer year) {
		List<DataSolsticeEquinoxDTO> result = new ArrayList<>();
		
		for (String solstice : SOLSTICES) {
			result.add(extractFor(year, solstice));
		}
		
		for (String equinox : EQUINOXES) {
			result.add(extractFor(year, equinox));
		}
		
		return result;
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
}
