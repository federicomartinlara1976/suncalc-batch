package net.bounceme.chronos.suncalc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class Registro<T> {

	@Getter
	@Setter
	T content;
}
