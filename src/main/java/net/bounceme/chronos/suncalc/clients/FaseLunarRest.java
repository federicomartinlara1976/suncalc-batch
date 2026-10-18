package net.bounceme.chronos.suncalc.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.dto.InputFaseLunarDTO;

@FeignClient(configuration = MoonPhaseFeignConfig.class, name = "moonPhaseClient", url = "${application.moonPhase.url}")
public interface FaseLunarRest {

	@PostMapping("/run-sync-get-dataset-items")
	List<FaseLunarDTO> detalle(@RequestBody InputFaseLunarDTO inputFaseLunarDTO);
}
