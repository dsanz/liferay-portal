package com.liferay.training.service.hello.client;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class HelloClientActivator implements BundleActivator {
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		System.out.println("[Hello Service Client] Starting Module...");

	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		System.out.println("[Hello Service Client] Stopping Module...");
	}
}
