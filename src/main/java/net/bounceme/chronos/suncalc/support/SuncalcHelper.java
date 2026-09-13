package net.bounceme.chronos.suncalc.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;

@Component
@Scope("prototype")
@Slf4j
public class SuncalcHelper {
	
	/**
	 * @param url
	 * @param coords
	 * @param date
	 * @return
	 */
	public String buildUrlQuery(String url, String coords, Date date) {
		String dateUrl = String.format(Constants.DATE_URL_FORMAT, date);
		String timeUrl = String.format(Constants.TIME_URL_FORMAT, date);
		
		return String.format(Constants.URL_FORMAT, url, coords, dateUrl, timeUrl);
	}

	/**
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Document retrieveDocument(String url) throws IOException {
		log.debug("Connecting to {}...", url);
		return Jsoup.connect(url).get();
	}

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
	
	public Differences createDifferences(TimeData nextData, TimeData prevData) {
		Differences d = new Differences();
		
		if (!Objects.isNull(nextData)) {
			d.setDawn(calculateDifference(nextData.getDawn(), prevData.getDawn()));
			d.setSunrise(calculateDifference(nextData.getSunrise(), prevData.getSunrise()));
			d.setCulmination(calculateDifference(nextData.getCulmination(), prevData.getCulmination()));
			d.setSunset(calculateDifference(nextData.getSunset(), prevData.getSunset()));
			d.setDusk(calculateDifference(nextData.getDusk(), prevData.getDusk()));
			
			d.setId(nextData.getId() + " - " + prevData.getId());
			d.setLastDate(nextData.getId());
		}
		
		return d;
	}

	private Long calculateDifference(Date nextData, Date prevData) {
		if (!Objects.isNull(nextData) && !Objects.isNull(prevData)) {
			Duration duration = Duration.between(convertToLocalTimeViaInstant(nextData),
					convertToLocalTimeViaInstant(prevData));
			return duration.toSeconds();
		}
		
		return 0L;
	}
}
