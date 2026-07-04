package net.bounceme.chronos.chguadalquivir;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloSelenium {
	public static void main(String[] args) {
		FirefoxOptions options = new FirefoxOptions();
		// options.addArguments("--headless");

		WebDriver driver = new FirefoxDriver(options);

		driver.get("https://suncalc.org/#/40.3863,-3.7117,19");
		//cleanAlert(driver);

		List<WebElement> elements = driver
				.findElements(By.cssSelector("div#legendeR > div#FensterF2 > table > tbody > tr"));
		for (WebElement element : elements) {
			try {
				List<WebElement> tds = element.findElements(By.cssSelector("td"));

				extractFields(tds);
			} catch (NoSuchElementException e) {
				log.error("ERROR: ", e);
			}
		}

		driver.quit();
	}

	private static void cleanAlert(WebDriver webDriver) {
		try {
			WebElement webAlert = webDriver.findElement(By.id("c-inr"));
			WebElement webButtons = webAlert.findElement(By.id("c-bns"));
			WebElement acceptButton = webButtons.findElement(By.id("c-p-bn"));

			acceptButton.click();
		} catch (NoSuchElementException e) {
		}
	}

	private static void extractFields(List<WebElement> tds) {
		try {
			WebElement nameElement = tds.get(0).findElement(By.cssSelector("acronym > span"));
			WebElement valueElement = tds.get(1).findElement(By.cssSelector("span"));

			String sField = nameElement.getText();
			sField = sField.substring(0, sField.length() - 1).toLowerCase();

			String sValue = valueElement.getText();

			log.info("{} = {}", sField, sValue);
		} catch (NoSuchElementException e) {
		}
	}
}