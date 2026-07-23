package net.bounceme.chronos.suncalc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestMcpClient {

	@Test
	public void test() {
		var client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://192.168.1.135:8091").build())
				.build();

		client.initialize();

		CallToolResult suncalc = client.callTool(new CallToolRequest("getCurrentTimeData", Map.of()));
		
		assertNotNull(suncalc);
		
		log.info("Result: {}", suncalc);
	}
}
