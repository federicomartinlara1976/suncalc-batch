package net.bounceme.chronos.suncalc.support.webdriver;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromeWebDriverProduct extends WebDriverProduct {

	@Override
	public void cleanAlert() {
		try {
			Alert alert = webDriver.switchTo().alert();
		
			String alertText = alert.getText();
	        log.info("Alert data: {}", alertText);
	        alert.accept();
		} catch (NoAlertPresentException e) {
			log.info("No alerts present");
		}
	}

}
