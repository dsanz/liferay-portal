package com.liferay.training.service.hello.client;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.liferay.training.service.hello.HelloService;

@Component(
		  immediate=true,
		  service = Object.class,
		  property = {
		    "osgi.command.function=say",
		    "osgi.command.scope=custom"
		  }
		)
public class HelloClientCommand {
	private HelloService helloService;

	public void say() {
		System.out.println(helloService.say());
	}

	public void say(String response) {
		System.out.println(helloService.say(response));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setHelloService(HelloService helloService) {
		System.out.println("[Hello Service Client]: setting hello service");
		this.helloService = helloService;
	}

	protected void unsetHelloService(HelloService helloService) {
		System.out.println("[Hello Service Client]: unsetting hello service");
		this.helloService = null;
	}
	
	@Activate
	protected void start() {
		if (helloService != null) {
			System.out.println("Hello Service command started");
	    }
	    else {
	    	System.out.println("No Hello Service available.");
	    }
	} 
}	
