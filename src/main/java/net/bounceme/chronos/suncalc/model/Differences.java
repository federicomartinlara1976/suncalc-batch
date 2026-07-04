package net.bounceme.chronos.suncalc.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Document(collection = "differences")
@ToString
@Data
public class Differences implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3641781709734044777L;
	
	@Id
    @Field("_id")
	private String id;
	
	@NotNull
	private String lastDate;

	@NotNull
	private Long dawn;
	
	@NotNull
	private Long sunrise;
	
	@NotNull
	private Long culmination;
	
	@NotNull
	private Long sunset;
	
	@NotNull
	private Long dusk;
}
