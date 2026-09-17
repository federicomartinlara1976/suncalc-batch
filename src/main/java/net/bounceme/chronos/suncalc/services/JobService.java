package net.bounceme.chronos.suncalc.services;

import java.util.List;

import net.bounceme.chronos.suncalc.model.ExecutionResult;

public interface JobService {
	
	/**
	 * @param name
	 */
	ExecutionResult run(String name);
	
	/**
	 * @param year
	 * @return
	 */
	ExecutionResult run(String name, Integer year);
	
	/**
	 * @param year
	 * @param month
	 * @return
	 */
	ExecutionResult run(String name, Integer year, Integer month);
	
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
