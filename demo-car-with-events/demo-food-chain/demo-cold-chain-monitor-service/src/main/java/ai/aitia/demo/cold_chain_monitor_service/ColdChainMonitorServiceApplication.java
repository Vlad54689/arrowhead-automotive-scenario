package ai.aitia.demo.cold_chain_monitor_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import eu.arrowhead.common.CommonConstants;

@SpringBootApplication
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE, ColdChainMonitorConstants.BASE_PACKAGE})
public class ColdChainMonitorServiceApplication {

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(ColdChainMonitorServiceApplication.class, args);
	}
}
