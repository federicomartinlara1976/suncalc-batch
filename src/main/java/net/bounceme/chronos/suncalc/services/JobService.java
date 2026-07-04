package net.bounceme.chronos.suncalc.services;

import java.util.List;

import net.bounceme.chronos.suncalc.model.ExecutionResult;

public interface JobService {
	
	/**
	 * @param name
	 * @throws Exception
	 */
	ExecutionResult run(String name) throws Exception;
	
	/**
	 * @return
	 */
	List<String> getJobNames();
	
	/**
	 * @param name
	 * @return
	 */
	String getJobScheduling(String name);

	/**
	 * @return
	 */
	List<String> getAllJobs();
}
