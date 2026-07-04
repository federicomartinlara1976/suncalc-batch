package net.bounceme.chronos.suncalc.support.processor;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.support.webdriver.WebDriverFactory;
import net.bounceme.chronos.suncalc.support.webdriver.WebDriverProduct;

@Component("seleniumDocumentProcessor")
@Slf4j
public class SeleniumDocumentProcessor implements DocumentProcessor {
	
	@Autowired
	@Qualifier("firefoxWebDriverFactory")
	private WebDriverFactory webDriverFactory;
	
	@Autowired
	@Qualifier("dateFormat")
	private SimpleDateFormat dateFormat;
	
	@Autowired
	@Qualifier("dateTimeFormat")
	private SimpleDateFormat dateTimeFormat;
	
	@Setter
	private String url;

	@Override
	public TimeData process() {
		TimeData timeData = new TimeData();
		
		Boolean status = Boolean.FALSE;
		Date date = new Date();
		timeData.setFecha(date);
		
		try (WebDriverProduct webDriverProduct = webDriverFactory.getDriverMethod()) {
			if (!Objects.isNull(webDriverProduct)) {
				String sDate = dateFormat.format(date);
				
				webDriverProduct.getWebDriver().get(url);
				
				webDriverProduct.cleanAlert();
				
				List<WebElement> elements = webDriverProduct.getWebDriver().findElements(By.cssSelector("div#legendeR > div#FensterF2 > table > tbody > tr"));
				for (WebElement element : elements) {
					try {
						List<WebElement> tds = element.findElements(By.cssSelector("td"));
						
						extractFields(timeData, sDate, tds);
					} catch (NoSuchElementException | ParseException e) {}
				}
				
				status = Boolean.TRUE;
			}
		} catch (IOException e) {}
		
		timeData.setStatus(status);
			
		return timeData;
	}

	private void extractFields(TimeData timeData, String sDate, List<WebElement> tds) throws ParseException {
		try {
			WebElement nameElement = tds.get(0).findElement(By.cssSelector("acronym > span"));
			WebElement valueElement = tds.get(1).findElement(By.cssSelector("span"));
			
			String sField = nameElement.getText();
			sField = sField.substring(0, sField.length()-1).toLowerCase();
			
			String sValue = valueElement.getText();
			
			if ("dawn".equals(sField)) {
				timeData.setDawn(dateTimeFormat.parse(sDate + " " + sValue));
			}
			
			if ("sunrise".equals(sField)) {
				timeData.setSunrise(dateTimeFormat.parse(sDate + " " + sValue));
			}
			
			if ("culmination".equals(sField)) {
				timeData.setCulmination(dateTimeFormat.parse(sDate + " " + sValue));
			}
			
			if ("sunset".equals(sField)) {
				timeData.setSunset(dateTimeFormat.parse(sDate + " " + sValue));
			}
			
			if ("dusk".equals(sField)) {
				timeData.setDusk(dateTimeFormat.parse(sDate + " " + sValue));
			}
			
			log.info("{} = {}", sField, sValue);
		} catch (NoSuchElementException e) {}
	}
}
