package net.bounceme.chronos.suncalc.services.impl;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;

@Service
public class McpSuncalcServiceImpl {
	
	@Autowired
	private McpSyncServerExchange exchange;
	
	@Autowired
	private SuncalcService suncalcService;

	@McpTool(description = "get dawn, sunrise, culmination, sunset and dusk for the current time")
	public TimeData getCurrentTimeData() {
		exchange.loggingNotification(LoggingMessageNotification.builder() // (3)
				.level(LoggingLevel.DEBUG)
				.data("Call getCurrentTimeData Tool with current time")
				.meta(Map.of()) // non null meta as a workaround for bug: ...
				.build());
		
		return suncalcService.getCurrentTimeData();
	}
}
