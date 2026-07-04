package net.bounceme.chronos.suncalc.reader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Component
public class DailyRegisterItemReader extends ItemStreamSupport implements ItemReader<TimeData> {
	
	@Autowired
	@Qualifier("timeFormat")
	private SimpleDateFormat timeFormat;
	
	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;
	
//	@Autowired
//	private SuncalcHelper helper;

	private List<TimeData> records;
	
	private Integer index = 0;
	
	@Override
	public void open(ExecutionContext executionContext) {
		initialize();
	}

	/**
	 * 
	 */
	@SneakyThrows
	private void initialize() {

		records = new ArrayList<>();
		
//		String urlQuery = helper.buildUrlQuery(url, coords, new Date());

		//documentProcessor.setUrl(url);
		records.add(documentProcessor.process());

		index = 0;
	}

	/**
	 *
	 */
	@Override
	public TimeData read() {
		TimeData nextElement = null;

		if (index < records.size()) {
			nextElement = records.get(index);
			index++;
		} else {
			index = 0;
		}

		return nextElement;
	}

}
