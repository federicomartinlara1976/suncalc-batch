package net.bounceme.chronos.suncalc.support.webdriver;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Deprecated(since = "1.0.2", forRemoval = true)
public class FirefoxWebDriverFactory implements WebDriverFactory {

	@Override
	@Deprecated(since = "1.0.2", forRemoval = true)
	public WebDriverProduct getDriverMethod() {
		FirefoxOptions options = new FirefoxOptions(); 
		options.addArguments("--headless");
		
		try (FirefoxWebDriverProduct product = new FirefoxWebDriverProduct()) {
			product.setWebDriver(new FirefoxDriver(options));
			return product;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

}
