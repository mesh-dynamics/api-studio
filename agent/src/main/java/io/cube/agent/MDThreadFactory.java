package io.cube.agent;

import java.util.concurrent.ThreadFactory;

public enum MDThreadFactory implements ThreadFactory {
	INSTANCE;

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r, "md-disruptor-thread");
		thread.setDaemon(true);
		return thread;
	}
}
