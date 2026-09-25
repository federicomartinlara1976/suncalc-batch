package net.bounceme.chronos.suncalc.services;

import java.util.List;

import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;

public interface AstronomiaService {

	List<DataSolsticeEquinoxDTO> calculateSolsticesEquinoxes(Integer year);
}
