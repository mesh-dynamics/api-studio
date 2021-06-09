/**
 * Copyright Cube I O
 */
package com.cube.ws;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * @author prasad
 *
 */
public class Binder extends AbstractBinder {

	/* (non-Javadoc)
	 * @see org.glassfish.hk2.utilities.binding.AbstractBinder#configure()
	 */
	@Override
	protected void configure() {
		bind(Config.class).to(Config.class).in(Singleton.class);
	}

}
