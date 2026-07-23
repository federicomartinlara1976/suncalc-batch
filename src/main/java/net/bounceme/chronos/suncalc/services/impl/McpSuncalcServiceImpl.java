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
	private SuncalcService suncalcService;

	@McpTool(description = "get dawn, sunrise, culmination, sunset and dusk for the current time")
	public TimeData getCurrentTimeData(McpSyncServerExchange exchange) {
		exchange.loggingNotification(LoggingMessageNotification.builder()
				.level(LoggingLevel.DEBUG)
				.data("Call getCurrentTimeData")
				.meta(Map.of()) // non null meata as a workaround for bug: ...
				.build());
		
		return suncalcService.getCurrentTimeData();
	}
}
