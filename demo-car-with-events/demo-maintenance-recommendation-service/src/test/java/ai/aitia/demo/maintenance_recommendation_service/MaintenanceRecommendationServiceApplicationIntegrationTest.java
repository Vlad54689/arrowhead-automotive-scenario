package ai.aitia.demo.maintenance_recommendation_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.demo.maintenance_recommendation_service.dto.MaintenanceRecommendationDTO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MaintenanceRecommendationServiceApplicationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() throws Exception {
		// Clean up any existing recommendations
		final String getAllResponse = mockMvc.perform(get("/maintenance-recommendation"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		final JsonNode recommendations = objectMapper.readTree(getAllResponse);
		for (JsonNode recommendation : recommendations) {
			if (recommendation.has("id")) {
				mockMvc.perform(delete("/maintenance-recommendation/" + recommendation.get("id").asInt()))
						.andExpect(status().isNoContent());
			}
		}
	}

	@Test
	public void testCreateMaintenanceRecommendation() throws Exception {
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setCarId(1);
		dto.setBrand("Toyota");
		dto.setRecommendationType("OIL_CHANGE");
		dto.setRecommendationDetails("Recommended oil change with HIGH priority.");
		dto.setPriority("HIGH");
		dto.setRecommendationTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendation = objectMapper.readTree(responseBody);

		assertNotNull(recommendation.get("id"));
		assertEquals(1, recommendation.get("carId").asInt());
		assertEquals("Toyota", recommendation.get("brand").asText());
		assertEquals("OIL_CHANGE", recommendation.get("recommendationType").asText());
		assertEquals("HIGH", recommendation.get("priority").asText());
	}

	@Test
	public void testGetMaintenanceRecommendationById() throws Exception {
		// First create a recommendation
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setCarId(2);
		dto.setBrand("Honda");
		dto.setRecommendationType("TIRE_ROTATION");
		dto.setRecommendationDetails("Recommended tire rotation with MEDIUM priority.");
		dto.setPriority("MEDIUM");
		dto.setRecommendationTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendation = objectMapper.readTree(responseBody);
		final int recommendationId = recommendation.get("id").asInt();

		// Then get it by ID
		final MvcResult getResult = mockMvc.perform(get("/maintenance-recommendation/" + recommendationId))
				.andExpect(status().isOk())
				.andReturn();

		final String getResultBody = getResult.getResponse().getContentAsString();
		final JsonNode retrievedRecommendation = objectMapper.readTree(getResultBody);

		assertEquals(2, retrievedRecommendation.get("carId").asInt());
		assertEquals("Honda", retrievedRecommendation.get("brand").asText());
		assertEquals("TIRE_ROTATION", retrievedRecommendation.get("recommendationType").asText());
		assertEquals("MEDIUM", retrievedRecommendation.get("priority").asText());
	}

	@Test
	public void testGetAllMaintenanceRecommendations() throws Exception {
		// Create two recommendations
		final MaintenanceRecommendationDTO recommendation1 = new MaintenanceRecommendationDTO();
		recommendation1.setCarId(3);
		recommendation1.setBrand("Ford");
		recommendation1.setRecommendationType("BRAKE_INSPECTION");
		recommendation1.setRecommendationDetails("Recommended brake inspection with LOW priority.");
		recommendation1.setPriority("LOW");
		recommendation1.setRecommendationTimestamp(System.currentTimeMillis());

		final MaintenanceRecommendationDTO recommendation2 = new MaintenanceRecommendationDTO();
		recommendation2.setCarId(4);
		recommendation2.setBrand("Chevrolet");
		recommendation2.setRecommendationType("FILTER_REPLACEMENT");
		recommendation2.setRecommendationDetails("Recommended filter replacement with HIGH priority.");
		recommendation2.setPriority("HIGH");
		recommendation2.setRecommendationTimestamp(System.currentTimeMillis());

		final String json1 = objectMapper.writeValueAsString(recommendation1);
		final String json2 = objectMapper.writeValueAsString(recommendation2);

		mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json1))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json2))
				.andExpect(status().isCreated());

		// Get all recommendations
		final MvcResult result = mockMvc.perform(get("/maintenance-recommendation"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendations = objectMapper.readTree(responseBody);

		assertEquals(2, recommendations.size());
	}

	@Test
	public void testSearchMaintenanceRecommendations() throws Exception {
		// Create a recommendation
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setCarId(5);
		dto.setBrand("BMW");
		dto.setRecommendationType("BATTERY_CHECK");
		dto.setRecommendationDetails("Recommended battery check with MEDIUM priority.");
		dto.setPriority("MEDIUM");
		dto.setRecommendationTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated());

		// Search by brand
		final Map<String, String> params = new HashMap<>();
		params.put("brand", "BMW");
		final MvcResult result = mockMvc.perform(get("/maintenance-recommendation/search")
				.param("brand", "BMW"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendations = objectMapper.readTree(responseBody);

		assertEquals(1, recommendations.size());
		assertEquals("BMW", recommendations.get(0).get("brand").asText());
	}

	@Test
	public void testUpdateMaintenanceRecommendation() throws Exception {
		// Create a recommendation
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setCarId(6);
		dto.setBrand("Audi");
		dto.setRecommendationType("OIL_CHANGE");
		dto.setRecommendationDetails("Recommended oil change with LOW priority.");
		dto.setPriority("LOW");
		dto.setRecommendationTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendation = objectMapper.readTree(responseBody);
		final int recommendationId = recommendation.get("id").asInt();

		// Update the recommendation
		final MaintenanceRecommendationDTO updatedDTO = new MaintenanceRecommendationDTO();
		updatedDTO.setCarId(6);
		updatedDTO.setBrand("Audi");
		updatedDTO.setRecommendationType("OIL_CHANGE");
		updatedDTO.setRecommendationDetails("Recommended oil change with HIGH priority.");
		updatedDTO.setPriority("HIGH");
		updatedDTO.setRecommendationTimestamp(System.currentTimeMillis());

		final String updatedJson = objectMapper.writeValueAsString(updatedDTO);
		final MvcResult updateResult = mockMvc.perform(put("/maintenance-recommendation/" + recommendationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updatedJson))
				.andExpect(status().isOk())
				.andReturn();

		final String updateResponseBody = updateResult.getResponse().getContentAsString();
		final JsonNode updatedRecommendation = objectMapper.readTree(updateResponseBody);

		assertEquals("Audi", updatedRecommendation.get("brand").asText());
		assertEquals("HIGH", updatedRecommendation.get("priority").asText());
	}

	@Test
	public void testDeleteMaintenanceRecommendation() throws Exception {
		// Create a recommendation
		final MaintenanceRecommendationDTO dto = new MaintenanceRecommendationDTO();
		dto.setCarId(7);
		dto.setBrand("Tesla");
		dto.setRecommendationType("OIL_CHANGE");
		dto.setRecommendationDetails("Recommended oil change with MEDIUM priority.");
		dto.setPriority("MEDIUM");
		dto.setRecommendationTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/maintenance-recommendation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendation = objectMapper.readTree(responseBody);
		final int recommendationId = recommendation.get("id").asInt();

		// Delete the recommendation
		mockMvc.perform(delete("/maintenance-recommendation/" + recommendationId))
				.andExpect(status().isNoContent());

		// Verify it's deleted
		mockMvc.perform(get("/maintenance-recommendation/" + recommendationId))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testRecommendCar() throws Exception {
		final Map<String, String> params = new HashMap<>();
		params.put("carId", "8");
		params.put("brand", "Mercedes");
		params.put("priority", "HIGH");

		final MvcResult result = mockMvc.perform(post("/maintenance-recommendation/recommend")
				.param("car-id", "8")
				.param("brand", "Mercedes")
				.param("priority", "HIGH"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode recommendation = objectMapper.readTree(responseBody);

		assertEquals(8, recommendation.get("carId").asInt());
		assertEquals("Mercedes", recommendation.get("brand").asText());
		assertEquals("HIGH", recommendation.get("priority").asText());
		assertNotNull(recommendation.get("recommendationType"));
		assertNotNull(recommendation.get("recommendationDetails"));
	}
}
