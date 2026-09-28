package net.bounceme.chronos.suncalc.services;

import java.util.List;

import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;

public interface AstronomiaService {

	List<DataSolsticeEquinoxDTO> calculateSolsticesEquinoxes(Integer year);
	
	FaseLunarDTO calculateFaseLunar(String date);
}
