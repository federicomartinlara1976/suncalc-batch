package net.bounceme.chronos.chguadalquivir;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.reader.DailyRegisterItemReader;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;

@SpringBootTest
@Slf4j
public class TestReaders {
	
	@Autowired
	private DailyRegisterItemReader dailyRegisterItemReader;
	
	@MockBean
	private ExecutionsRepository executionsRepository;
	
	@Value("${application.importJob.url}")
	private String url;
	
	@Test
	public void testDailyRegisterItemReader() {
		try {
			TimeData embalse;
			while ((embalse = dailyRegisterItemReader.read()) != null) {
				log.info(embalse.toString());
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			Assert.fail(e.getMessage());
		}
	}
	
	private List<Execution> buildExecutions() {
		List<Execution> executions = new ArrayList<>();
		
		Execution execution = Execution.builder().id("2023-01-23").value(1).executionTime(1896L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-24").value(1).executionTime(1923L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-25").value(1).executionTime(1642L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-26").value(1).executionTime(1828L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-27").value(1).executionTime(1805L).build();
		executions.add(execution);
		
		execution = Execution.builder().id("2023-01-28").value(1).executionTime(1665L).build();
		executions.add(execution);
		
		return executions;
	}
}
