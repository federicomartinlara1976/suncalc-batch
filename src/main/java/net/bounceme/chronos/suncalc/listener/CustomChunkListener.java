package net.bounceme.chronos.suncalc.listener;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomChunkListener implements ChunkListener {

	@Override
	public void beforeChunk(ChunkContext context) {
		log.debug("Before chunk");
	}

	@Override
	public void afterChunk(ChunkContext context) {
		log.debug("After chunk");
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		log.debug("After chunk: error");
	}

}
