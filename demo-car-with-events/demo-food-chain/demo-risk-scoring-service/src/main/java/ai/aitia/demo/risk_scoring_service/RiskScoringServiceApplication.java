package ai.aitia.demo.risk_scoring_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import ai.aitia.demo.risk_scoring_service.subscriber.ConfigEventProperites;
import eu.arrowhead.common.CommonConstants;

@SpringBootApplication
@EnableConfigurationProperties(ConfigEventProperites.class)
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE, RiskScoringConstants.BASE_PACKAGE})
public class RiskScoringServiceApplication {

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(RiskScoringServiceApplication.class, args);
	}
}
