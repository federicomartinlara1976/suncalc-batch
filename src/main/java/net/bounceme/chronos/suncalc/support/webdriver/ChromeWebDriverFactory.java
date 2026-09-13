package net.bounceme.chronos.suncalc.support.webdriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated
 */
@Slf4j
@Deprecated(since = "1.0.2", forRemoval = true)
public class ChromeWebDriverFactory implements WebDriverFactory {
	
	@Value("${application.importTimes.chromedriver}")
	private String chromedriver;
	
	@Value("${application.importTimes.navigator}")
	private String navigator;

	/**
	 * @deprecated
	 */
	@Deprecated(since = "1.0.2", forRemoval = true)
	@Override
	public WebDriverProduct getDriverMethod() {
		ChromeOptions options = new ChromeOptions(); 
		options.setBinary(navigator);
		options.addArguments("--remote-allow-origins=*", "--headless");
		
		try (ChromeWebDriverProduct product = new ChromeWebDriverProduct()) {
			product.setWebDriver(new ChromeDriver(options));
			return product;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

}
