package net.bounceme.chronos.suncalc.model;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Document(collection = "#{@repositoryCollectionCustom.getCollectionName()}")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class TimeData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3641781709734044777L;
	
	@Id
    @Field("_id")
	private String id;

	private Date dawn;
	
	private Date sunrise;
	
	private Date culmination;
	
	private Date sunset;
	
	private Date dusk;
	
	@Transient
	private Date fecha;
	
	@Transient
	@AssertTrue
	private Boolean status;
}
