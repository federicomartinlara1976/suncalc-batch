package net.bounceme.chronos.suncalc.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class Registro<T> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Getter
	@Setter
	private transient T content;
}
