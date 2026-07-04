package net.bounceme.chronos.suncalc.support.webdriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChromeWebDriverFactory implements WebDriverFactory {
	
	@Value("${application.importTimes.chromedriver}")
	private String chromedriver;
	
	@Value("${application.importTimes.navigator}")
	private String navigator;

	@Override
	public WebDriverProduct getDriverMethod() {
		try {
			ChromeOptions options = new ChromeOptions(); 
			options.setBinary(navigator);
			options.addArguments("--remote-allow-origins=*", "--headless");
			
			ChromeWebDriverProduct product = new ChromeWebDriverProduct();
			product.setWebDriver(new ChromeDriver(options));
			return product;
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

}
