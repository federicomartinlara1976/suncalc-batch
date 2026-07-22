package net.bounceme.chronos.suncalc.services.impl;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;

@Service
public class McpSuncalcServiceImpl {
	
	@Autowired
	private SuncalcService suncalcService;

	@McpTool(description = "get dawn, sunrise, culmination, sunset and dusk for the current time")
	public TimeData getCurrentTimeData() {
		return suncalcService.getCurrentTimeData();
	}
}
