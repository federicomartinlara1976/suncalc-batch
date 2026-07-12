package net.bounceme.chronos.suncalc.services;

import java.util.List;
import java.util.Optional;

import net.bounceme.chronos.suncalc.model.TimeData;

public interface SuncalcService {

	TimeData getCurrentTimeData();

	Optional<TimeData> getTimeDataByDate(String date);
	
	List<TimeData> getByRangeDate(String initDate, String endDate);
}
