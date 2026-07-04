package net.bounceme.chronos.suncalc.support.processor;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

@Component("jsoupDocumentProcessor")
@Slf4j
public class JSoupDocumentProcessor implements DocumentProcessor {
	
	@Autowired
	private SuncalcHelper helper;
	
	private Document document;

	@Override
	@SneakyThrows
	public void setUrl(String url) {
		document = helper.retrieveDocument(url);
	}

	@Override
	public TimeData process() {
		Elements elements = document.select("div#legendeR > div#FensterF2 > table > tbody > tr");
		TimeData timeData = new TimeData();
		
		for (int i = 0; i < elements.size(); i++) {
			Elements tds = elements.get(i).select("td");
			String field = extractField(tds.get(0));
			String value = extractValue(tds.get(1));
			log.info("{} = {}", field, value);
			
			
		}
		
		return timeData;
	}

	private String extractValue(Element element) {
		String value = element.select("span").text();
		return value;
	}

	private String extractField(Element element) {
		String value = element.select("acronym > span").text();
		return value.substring(0, value.length()-1);
	}
}
