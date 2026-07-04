package net.bounceme.chronos.suncalc.support.webdriver;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FirefoxWebDriverFactory implements WebDriverFactory {

	@Override
	public WebDriverProduct getDriverMethod() {
		try {
			FirefoxOptions options = new FirefoxOptions(); 
			options.addArguments("--headless");
			
			FirefoxWebDriverProduct product = new FirefoxWebDriverProduct();
			product.setWebDriver(new FirefoxDriver(options));
			return product;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

}
