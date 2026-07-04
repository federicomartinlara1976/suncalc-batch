package net.bounceme.chronos.suncalc.support.webdriver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirefoxWebDriverProduct extends WebDriverProduct {

	@Override
	public void cleanAlert() {
		log.info("cleanAlert");
	}
}
