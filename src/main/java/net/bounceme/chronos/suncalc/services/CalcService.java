package net.bounceme.chronos.suncalc.services;

import java.math.BigDecimal;
import java.util.List;

public interface CalcService {
	
	void addPath(String path);

	void resetPath();
	
	void clearEnvironment();
	
	void execute(String cmd);
	
	void passVariable(String name, BigDecimal value);
	
	void passVariable(String name, BigDecimal[] value);
	
	void passVariable(String name, BigDecimal[][] value);
	
	void passVariable(String name, Integer value);
	
	void terminate();
	
	List<String> getVars();
	
	BigDecimal getScalar(String name);
	
	Integer getIntScalar(String name);
	
	BigDecimal[] getArray(String name);
	
	BigDecimal[][] getMatrix(String name);
	
	String getString(String name);
}
