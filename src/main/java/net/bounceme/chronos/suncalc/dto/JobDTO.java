package net.bounceme.chronos.suncalc.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class JobDTO<T> {

	@Getter
	@Setter
	private transient T content;
}
