package net.bounceme.chronos.suncalc.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.services.AstronomiaService;
import net.bounceme.chronos.suncalc.services.CalcService;

@Service
public class AstronomiaServiceImpl implements AstronomiaService {
	
	private static final String[] EQUINOXES = {"march_equinox", "september_equinox"};
	private static final String[] SOLSTICES = {"june_solstice", "december_solstice"};
	private static final String HOUR_ZONE = "Europe/Madrid";
	
	@Autowired
	private CalcService calcService;
	
	@Override
	public void calculateSolsticesEquinoxes(Integer year) {
		
		for (String solstice : SOLSTICES) {
			executeFor(year, solstice);
		}
		
		for (String equinox : EQUINOXES) {
			executeFor(year, equinox);
		}
	}
	
	private void executeFor(Integer year, String item) {
		final String CMD_TEMPLATE = "[JDE, utc, local, off] = equinoccio_solsticio(%d, '%s', '%s')";
		String cmd = String.format(CMD_TEMPLATE, year, item, HOUR_ZONE);
		calcService.execute(cmd);
	}

}
