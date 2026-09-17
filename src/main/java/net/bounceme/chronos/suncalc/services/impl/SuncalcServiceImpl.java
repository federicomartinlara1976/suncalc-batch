package net.bounceme.chronos.suncalc.services.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;
import net.bounceme.chronos.suncalc.services.SuncalcService;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@Service
public class SuncalcServiceImpl implements SuncalcService {

	@Autowired
	@Qualifier("feignDocumentProcessor")
	private DocumentProcessor documentProcessor;

	@Value("${application.importTimes.collection}")
	private String collection;

	@Autowired
	private TimeDataRepository timeDataRepository;

	@Autowired
	private RepositoryCollectionCustom repositoryCollectionCustom;

	@Autowired
	private SimpleDateFormat dateFormat;

	@Override
	public TimeData getCurrentTimeData() {
		Date d = new Date();
		String id = dateFormat.format(d);
		repositoryCollectionCustom.setCollectionName(collection);
		
		return timeDataRepository.findById(id).orElseGet(() -> {
			TimeData t = documentProcessor.process();
			t.setId(id);
			t.setFecha(d);
			t.setStatus(true);
			return t;
		});
	}

	@Override
	public Optional<TimeData> getTimeDataByDate(String date) {
		repositoryCollectionCustom.setCollectionName(collection);
		
		return timeDataRepository.findById(date);
	}

	@Override
	public List<TimeData> getByRangeDate(String initDate, String endDate) {
		repositoryCollectionCustom.setCollectionName(collection);
		return timeDataRepository.listRegistros(initDate, endDate, Sort.by(Sort.Direction.ASC, "_id"));
	}

}
