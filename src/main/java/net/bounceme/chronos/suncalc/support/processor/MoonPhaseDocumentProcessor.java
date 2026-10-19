package net.bounceme.chronos.suncalc.support.processor;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.clients.FaseLunarRest;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.dto.InputFaseLunarDTO;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.model.Registro;

@Component("moonPhaseDocumentProcessor")
@Slf4j
public class MoonPhaseDocumentProcessor extends BaseDocumentProcessor<MoonData> {
	
	@Autowired
	private FaseLunarRest faseLunarRest;

	@Override
	public Registro<MoonData> process() {
		return obtainData(dateFormat.format(new Date()));
	}

	@SneakyThrows
	private Registro<MoonData> obtainData(String sDate) {
		MoonData moonData = new MoonData();
		
		Boolean status = Boolean.FALSE;
		moonData.setFecha(dateFormat.parse(sDate));
		
		InputFaseLunarDTO inputFaseLunarDTO = InputFaseLunarDTO.builder()
				.startDate(sDate)
				.latitude(latitude)
				.longitude(longitude)
				.days(1)
				.timezone(timeZone)
				.build();
		
		List<FaseLunarDTO> response = faseLunarRest.detalle(inputFaseLunarDTO);
		
		if (CollectionUtils.isNotEmpty(response)) {
			FaseLunarDTO faseLunarDTO = response.get(0);
			moonData.setId(dateFormat.format(faseLunarDTO.getDate()));
			moonData.setWeekday(faseLunarDTO.getWeekday());
			moonData.setPhaseName(faseLunarDTO.getPhaseName());
			moonData.setPhaseEmoji(faseLunarDTO.getPhaseEmoji());
			moonData.setIlluminationPercent(faseLunarDTO.getIlluminationPercent());
			moonData.setMoonAgeDays(faseLunarDTO.getMoonAgeDays());
			moonData.setDistanceKm(faseLunarDTO.getDistanceKm());
			moonData.setPrincipalPhase(faseLunarDTO.getPrincipalPhase());
			moonData.setPrincipalPhaseTime(faseLunarDTO.getPrincipalPhaseTime());
			moonData.setMoonrise(faseLunarDTO.getMoonrise());
			moonData.setMoonset(faseLunarDTO.getMoonset());
			
			status = Boolean.TRUE;
		}
		
		moonData.setStatus(status);
			
		Registro<MoonData> registro = new Registro<>();
		registro.setContent(moonData);
		return registro;
	}

	@Override
	public Registro<MoonData> process(String date) {
		return obtainData(date);
	}
}
