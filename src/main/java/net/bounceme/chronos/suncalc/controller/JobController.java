package net.bounceme.chronos.suncalc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.MonthYearDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;
import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.model.Task;
import net.bounceme.chronos.suncalc.services.JobService;

@RestController
@RequestMapping("/suncalc-batch/jobs")
@Slf4j
public class JobController {

	@Autowired
	private JobService jobService;
	
	@Autowired
	private JobFacade jobFacade;

	@PostMapping("/execute")
	@SneakyThrows
	public ResponseEntity<Map<String, Object>> executeTask(@Valid @RequestBody Task task, BindingResult result) {
		Map<String, Object> response = new HashMap<>();

		log.info("Ejecutar: {}", task.getName());
		ExecutionResult resultado = jobService.run(task.getName());
		response.put("resultado", resultado);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Ejecuta una tarea que recupera los datos de un mes. Se lanza de forma asíncrona, ya que 
	 * su tiempo de ejecución es indeterminado. 
	 * 
	 * @param task
	 * @param result
	 * @return
	 */
	@PostMapping("/executeByMonth/{year}/{month}")
	@SneakyThrows
	public ResponseEntity<Map<String, Object>> executeTaskByMonthAndYear(@PathVariable Integer year, @PathVariable Integer month) {
		Map<String, Object> response = new HashMap<>();

		log.info("Ejecutar: byMonthAndYear with {}/{}", month, year);
		
		MonthYearDTO monthYearDTO = MonthYearDTO.builder().month(month).year(year).build();
		
		JobDTO<MonthYearDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(monthYearDTO);
		jobFacade.publishJob(jobDTO);
		
		response.put("message", "Tarea en ejecución");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/scheduling")
	public ResponseEntity<Map<String, Object>> schedulingJob(@Valid @RequestBody Task task, BindingResult result) {
		Map<String, Object> response = new HashMap<>();

		String scheduling = jobService.getJobScheduling(task.getName());
		log.debug("Cron de {}: {}", task.getName(), scheduling);
		response.put("scheduling", scheduling);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("")
	public ResponseEntity<Map<String, Object>> getJobs() {
		Map<String, Object> response = new HashMap<>();

		List<String> jobs = jobService.getAllJobs();
		response.put("jobs", jobs);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
