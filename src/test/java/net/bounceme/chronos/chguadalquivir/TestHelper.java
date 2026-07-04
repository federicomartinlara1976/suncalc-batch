package net.bounceme.chronos.chguadalquivir;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

@SpringBootTest
@Slf4j
public class TestHelper {
	
	@Autowired
	private SuncalcHelper helper;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Value("${application.importJob.url}")
	private String url;
	
	@Test
	public void testRound() {
		Double rounded = helper.round(1.2345, 2);
		assertNotNull(rounded);
	}
	
	@Test
	public void testSubtractDays() {
		Date modified = helper.subtractDays(new Date(), 5);
		assertNotNull(modified);
	}
}
