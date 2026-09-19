package net.bounceme.chronos.suncalc.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;

@UtilityClass
@Slf4j
public class SuncalcHelper {

	/**
	 * @param d
	 * @param numOfDays
	 * @return
	 */
	public Date subtractDays(Date date, Integer numOfDays) {
		LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

		// minus numOfDays
		localDateTime = localDateTime.minusDays(numOfDays);

		// convert LocalDateTime to date
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * @param input
	 * @param scale
	 * @return
	 */
	public Double round(Double input, Integer scale) {
		BigDecimal bd = BigDecimal.valueOf(input).setScale(scale, RoundingMode.HALF_EVEN);
		return bd.doubleValue();
	}
	
	/**
	 * @param dateToConvert
	 * @return
	 */
	public LocalTime convertToLocalTimeViaInstant(Date dateToConvert) {
	    return dateToConvert.toInstant()
	      .atZone(ZoneId.systemDefault())
	      .toLocalTime();
	}
	
	public Integer obtenerDia(Date fecha) {
	    return fecha.toInstant()
	                .atZone(ZoneId.systemDefault())
	                .getDayOfMonth(); // 1-31
	}
	
	public Integer obtenerMes(Date fecha) {
	    return fecha.toInstant()
	                .atZone(ZoneId.systemDefault())
	                .getMonthValue(); // 1 = enero, 12 = diciembre
	}
	
	public Integer obtenerAnio(Date fecha) {
	    return fecha.toInstant()
	                .atZone(ZoneId.systemDefault())
	                .getYear();
	}
	
	public Optional<Differences> createDifferences(TimeData nextData, TimeData prevData) {
		
		if (!Objects.isNull(nextData)) {
			Differences d = new Differences();
			
			d.setDawn(calculateDifference(nextData.getDawn(), prevData.getDawn()));
			d.setSunrise(calculateDifference(nextData.getSunrise(), prevData.getSunrise()));
			d.setCulmination(calculateDifference(nextData.getCulmination(), prevData.getCulmination()));
			d.setSunset(calculateDifference(nextData.getSunset(), prevData.getSunset()));
			d.setDusk(calculateDifference(nextData.getDusk(), prevData.getDusk()));
			
			d.setId(nextData.getId() + " - " + prevData.getId());
			d.setLastDate(nextData.getId());
			
			return Optional.of(d);
		}
		
		return Optional.empty();
	}

	private Long calculateDifference(Date nextData, Date prevData) {
		if (!Objects.isNull(nextData) && !Objects.isNull(prevData)) {
			Duration duration = Duration.between(convertToLocalTimeViaInstant(nextData),
					convertToLocalTimeViaInstant(prevData));
			return duration.toSeconds();
		}
		
		return 0L;
	}
	
	public Integer getDiasDelMes(Integer month, Integer year) {
	    switch (month) {
	        case 1, 3, 5, 7, 8, 10, 12:
	            return 31;
	        case 4, 6, 9, 11:
	            return 30;
	        case 2:
	            return esBisiesto(year) ? 29 : 28;
	        default:
	            throw new IllegalArgumentException("Mes inválido: " + month);
	    }
	}

	private boolean esBisiesto(Integer year) {
	    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
	}
	
	public String normalize(Integer num) {
		return (num < 10) ? "0" + num.toString() : num.toString();
	}

}
