package net.bounceme.chronos.chguadalquivir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestFuture {
	
	@Test
	public void testFuture() {
		try {
			Future<Integer> future = new SquareCalculator().calculate(10);
	
			while(!future.isDone()) {
			    log.info("Calculating...");
			    Thread.sleep(300);
			}
	
			Integer result = future.get();
			log.info("Result: {}", result);
			
			assertEquals(100, result);
		} catch (InterruptedException | ExecutionException e) {
			log.error(e.getMessage());
			Assert.fail(e.getMessage());
		}
	}
}
