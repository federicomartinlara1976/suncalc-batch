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
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;
import net.bounceme.chronos.suncalc.services.JobService;

@RestController
@RequestMapping("/suncalc-batch/jobs")
@Slf4j
public class JobController {

	private static final String IN_PROGRESS = "Tarea en ejecución";

	private static final String MESSAGE = "message";

	@Autowired
	private JobService jobService;
	
	@Autowired
	private JobFacade jobFacade;

	@PostMapping("/execute")
	@SneakyThrows
	public ResponseEntity<Map<String, Object>> executeTask(@Valid @RequestBody TaskDTO task) {
		Map<String, Object> response = new HashMap<>();

		log.debug("Ejecutar: {}", task.getName());
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(task);
		jobFacade.publishJob(jobDTO);
		
		response.put(MESSAGE, IN_PROGRESS);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Ejecuta una tarea que recupera los datos de un mes. Se lanza de forma asíncrona, ya que 
	 * su tiempo de ejecución es indeterminado. 
	 * 
	 * @param year
	 * @param month
	 * @param result
	 * @return
	 */
	@PostMapping("/executeByMonth/{year}/{month}")
	@SneakyThrows
	public ResponseEntity<Map<String, Object>> executeTaskByMonthAndYear(@PathVariable Integer year, @PathVariable Integer month) {
		Map<String, Object> response = new HashMap<>();

		log.debug("Ejecutar: byMonthAndYear with {}/{}", month, year);
		
		TaskDTO taskDTO = TaskDTO.builder().name("importByMonth").month(month).year(year).build();
		
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(taskDTO);
		jobFacade.publishJob(jobDTO);
		
		response.put(MESSAGE, IN_PROGRESS);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/**
	 * Ejecuta una tarea que recupera los datos de un mes. Se lanza de forma asíncrona, ya que 
	 * su tiempo de ejecución es indeterminado. 
	 * 
	 * @param year
	 * @param month
	 * @param result
	 * @return
	 */
	@PostMapping("/recalculate/{year}")
	@SneakyThrows
	public ResponseEntity<Map<String, Object>> recalculateByYear(@PathVariable Integer year) {
		Map<String, Object> response = new HashMap<>();

		log.debug("Ejecutar: recalculateByYear with {}", year);
		
		TaskDTO taskDTO = TaskDTO.builder().name("recalculateByYear").year(year).build();
		
		JobDTO<TaskDTO> jobDTO = new JobDTO<>();
		jobDTO.setContent(taskDTO);
		jobFacade.publishJob(jobDTO);
		
		response.put(MESSAGE, IN_PROGRESS);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/scheduling")
	public ResponseEntity<Map<String, Object>> schedulingJob(@Valid @RequestBody TaskDTO task, BindingResult result) {
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
