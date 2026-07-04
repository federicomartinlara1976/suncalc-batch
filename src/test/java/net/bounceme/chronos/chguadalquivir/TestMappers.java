package net.bounceme.chronos.chguadalquivir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

@SpringBootTest
@Slf4j
public class TestMappers {
	
	@Autowired
	private SuncalcHelper helper;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Value("${application.importJob.url}")
	private String url;
}
