package ai.aitia.demo.supply_chain_trust_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import ai.aitia.demo.supply_chain_trust_service.subscriber.ConfigEventProperites;
import eu.arrowhead.common.CommonConstants;

@SpringBootApplication
@EnableConfigurationProperties(ConfigEventProperites.class)
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE, SupplyChainTrustConstants.BASE_PACKAGE})
public class SupplyChainTrustServiceApplication {

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(SupplyChainTrustServiceApplication.class, args);
	}
}
