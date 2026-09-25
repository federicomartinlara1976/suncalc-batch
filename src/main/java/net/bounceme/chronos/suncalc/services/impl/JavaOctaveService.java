package net.bounceme.chronos.suncalc.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.OctaveUtils;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveString;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bounceme.chronos.suncalc.services.CalcService;
import net.bounceme.chronos.utils.calc.converters.Converter;
import net.bounceme.chronos.utils.calc.converters.OctaveDoubleToArray;
import net.bounceme.chronos.utils.calc.converters.OctaveDoubleToMatrix;
import net.bounceme.chronos.utils.calc.dto.MatrixDTO;
import net.bounceme.chronos.utils.calc.dto.VectorDTO;

@Service
@Slf4j
public class JavaOctaveService implements CalcService {

	private static final Integer SCALE = 2;
	
	private OctaveEngine octave;

	private OctaveDoubleToArray octaveDoubleToArray;

	private OctaveDoubleToMatrix octaveDoubleToMatrix;

	public JavaOctaveService() {
		octave = new OctaveEngineFactory().getScriptEngine();
		
		// Load the astronomy package
		octave.eval("pkg load astronomia");
				
		// Test if load is correct by recovering the package version
		octave.eval("astronomia_version = astronomia_version()");
		String packageVersion = getString("astronomia_version");
		log.info("Astronomia version: {}", packageVersion);
		
		octaveDoubleToArray = new OctaveDoubleToArray();
		octaveDoubleToMatrix = new OctaveDoubleToMatrix();
	}

	@SneakyThrows
	public void addPath(String path) {
		String cmd = String.format("addpath('%s')", path);
		octave.eval(cmd);
	}

	@SneakyThrows
	public void resetPath() {
		octave.eval("restoredefaultpath();");
	}

	@SneakyThrows
	public void clearEnvironment() {
		octave.eval("clear()");
	}

	@SneakyThrows
	public void execute(String cmd) {
		log.debug("Ejecutar comando: {}", cmd);
		octave.eval(cmd);
	}

	/**
	 * @param name
	 * @param value
	 */
	@SneakyThrows
	public void passVariable(String name, BigDecimal value) {
		log.debug("Pasando variable {} con valor {}", name, value.doubleValue());
		String cmd = String.format("%s=%s", name, value.toString());
		octave.eval(cmd);
	}

	/**
	 * @param name
	 * @param value
	 */
	@SneakyThrows
	public void passVariable(String name, Integer value) {
		log.debug("Pasando variable {} con valor {}", name, value.doubleValue());
		String cmd = String.format("%s=%s", name, value.toString());
		octave.eval(cmd);
	}

	@Override
	@SneakyThrows
	public void passVariable(String name, BigDecimal[] value) {
		VectorDTO vectorDTO = new VectorDTO(name, value);
		String cmdVar = vectorDTO.toString();
		log.debug("Pasando variable {}", cmdVar);
		octave.eval(cmdVar);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bounceme.chronos.testjopas.services.CalcService#passVariable(java.lang.
	 * String, java.math.BigDecimal[][])
	 */
	@Override
	@SneakyThrows
	public void passVariable(String name, BigDecimal[][] value) {
		MatrixDTO matrixDTO = new MatrixDTO(name, value);
		String cmdVar = matrixDTO.toString();
		log.debug("Pasando variable {}", cmdVar);
		octave.eval(cmdVar);
	}

	/**
	 * @param name
	 * @param value
	 */
	@SneakyThrows
	public void terminate() {
		octave.close();
	}

	@Override
	public List<String> getVars() {
		return OctaveUtils.listVars(octave).stream().toList();
	}

	@Override
	public BigDecimal getScalar(String name) {
		Converter<OctaveDouble, BigDecimal> converter = s -> BigDecimal.valueOf(s.getData()[0])
                .setScale(SCALE, RoundingMode.HALF_UP);
		return converter.apply(octave.get(OctaveDouble.class, name));
	}

	@Override
	public Integer getIntScalar(String name) {
		Converter<OctaveDouble, Integer> converter = s -> BigDecimal.valueOf(s.getData()[0])
                .setScale(0, RoundingMode.HALF_UP) // Redondeo estándar
                .intValueExact();
		return converter.apply(octave.get(OctaveDouble.class, name));
	}

	@Override
	public BigDecimal[] getArray(String name) {
		return octaveDoubleToArray.apply(octave.get(OctaveDouble.class, name));
	}

	@Override
	public BigDecimal[][] getMatrix(String name) {
		return octaveDoubleToMatrix.apply(octave.get(OctaveDouble.class, name));
	}

	@Override
	public String getString(String name) {
		Converter<OctaveString, String> converter = s -> s.getString();
		return converter.apply(octave.get(OctaveString.class, name));
	}
}
