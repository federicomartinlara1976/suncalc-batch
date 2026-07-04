package net.bounceme.chronos.suncalc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.bounceme.chronos.suncalc.model.Status;

@RestController
@RequestMapping("/suncalc-batch")
public class IndexController {

	@Value("${spring.application.name}")
	private String applicationName;
	
	@Value("${spring.application.description}")
	private String applicationDescription;

	@Value("${server.port}")
	private Integer port;
	
	@Value("${eureka.instance.instance-id}")
	private String instanceId;

	/**
	 * @return
	 */
	@GetMapping("/status")
	public ResponseEntity<Status> status() {
		Status status = Status.builder()
				.applicationName(applicationName)
				.description(applicationDescription)
				.version(System.getProperty("java.version"))
				.platform(System.getProperty("os.name"))
				.instanceId(instanceId)
				.port(port)
				.response("OK").build();
		
		return new ResponseEntity<>(status, HttpStatus.OK);
	}
}
