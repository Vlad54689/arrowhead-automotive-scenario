package ai.aitia.demo.risk_scoring_service.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiny helper for the chain's flat string event payload (format: "key=val;key=val;...").
 * Kept deliberately minimal - the scenario is about the multi-hop chain and its measurement,
 * not about serialization.
 */
public final class ChainPayload {

	//-------------------------------------------------------------------------------------------------
	public static Map<String, String> parse(final String payload) {
		final Map<String, String> map = new HashMap<>();
		if (payload == null || payload.isBlank()) {
			return map;
		}
		for (final String pair : payload.split(";")) {
			final int eq = pair.indexOf('=');
			if (eq > 0) {
				map.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
			}
		}
		return map;
	}

	//-------------------------------------------------------------------------------------------------
	private ChainPayload() {
		throw new UnsupportedOperationException();
	}
}
