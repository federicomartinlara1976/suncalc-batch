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
public class MoonData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3641781709734044777L;
	
	@Id
    @Field("_id")
	private String id;

	private String weekday;

	private String phaseName;
	
	private String phaseEmoji;

	private Float illuminationPercent;

	private Float moonAgeDays;

	private Float distanceKm;
        
	private String principalPhase;
        
	private String principalPhaseTime;
        
	private String moonrise;
        
	private String moonset;
	
	@Transient
	private Date fecha;
	
	@Transient
	@AssertTrue
	private Boolean status;
}
