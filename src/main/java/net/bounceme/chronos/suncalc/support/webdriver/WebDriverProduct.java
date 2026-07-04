package net.bounceme.chronos.suncalc.support.webdriver;

import java.io.Closeable;
import java.io.IOException;

import org.openqa.selenium.WebDriver;

import lombok.Getter;
import lombok.Setter;

public abstract class WebDriverProduct implements Closeable {
	
	@Getter
	@Setter
	protected WebDriver webDriver;
	
	public abstract void cleanAlert();
	
	@Override
	public void close() throws IOException {
		webDriver.quit();
	}
}
