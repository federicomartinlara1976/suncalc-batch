package net.bounceme.chronos.suncalc.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.services.AstronomiaService;
import net.bounceme.chronos.suncalc.services.CalcService;

@Service
public class AstronomiaServiceImpl implements AstronomiaService {
	
	@Autowired
	private CalcService calcService;

}
