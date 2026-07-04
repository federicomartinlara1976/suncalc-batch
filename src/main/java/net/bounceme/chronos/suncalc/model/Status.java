package net.bounceme.chronos.suncalc.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class Status implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6605836896150803431L;
	
	private String applicationName;
	
	private String version;
	
	private String platform;
	
	private String response;
	
	private String description;
	
	private Integer port;
	
	private String instanceId;
}
