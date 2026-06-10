package ai.aitia.demo.traceability_log_service;

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

import ai.aitia.demo.traceability_log_service.dto.TraceabilityLogDTO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TraceabilityLogServiceApplicationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() throws Exception {
		// Clean up any existing logs
		final String getAllResponse = mockMvc.perform(get("/traceability-logs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		final JsonNode logs = objectMapper.readTree(getAllResponse);
		for (JsonNode log : logs) {
			if (log.has("id")) {
				mockMvc.perform(delete("/traceability-logs/" + log.get("id").asInt()))
						.andExpect(status().isNoContent());
			}
		}
	}

	@Test
	public void testCreateTraceabilityLog() throws Exception {
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(1);
		dto.setBrand("Toyota");
		dto.setEventType("INSPECTION_COMPLETED");
		dto.setEventDetails("Quality inspection completed successfully.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode log = objectMapper.readTree(responseBody);

		assertNotNull(log.get("id"));
		assertEquals(1, log.get("carId").asInt());
		assertEquals("Toyota", log.get("brand").asText());
		assertEquals("INSPECTION_COMPLETED", log.get("eventType").asText());
	}

	@Test
	public void testGetTraceabilityLogById() throws Exception {
		// First create a log
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(2);
		dto.setBrand("Honda");
		dto.setEventType("MAINTENANCE_COMPLETED");
		dto.setEventDetails("Maintenance completed successfully.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode log = objectMapper.readTree(responseBody);
		final int logId = log.get("id").asInt();

		// Then get it by ID
		final MvcResult getResult = mockMvc.perform(get("/traceability-logs/" + logId))
				.andExpect(status().isOk())
				.andReturn();

		final String getResultBody = getResult.getResponse().getContentAsString();
		final JsonNode retrievedLog = objectMapper.readTree(getResultBody);

		assertEquals(2, retrievedLog.get("carId").asInt());
		assertEquals("Honda", retrievedLog.get("brand").asText());
		assertEquals("MAINTENANCE_COMPLETED", retrievedLog.get("eventType").asText());
	}

	@Test
	public void testGetAllTraceabilityLogs() throws Exception {
		// Create two logs
		final TraceabilityLogDTO log1 = new TraceabilityLogDTO();
		log1.setCarId(3);
		log1.setBrand("Ford");
		log1.setEventType("INSPECTION_COMPLETED");
		log1.setEventDetails("Quality inspection completed.");
		log1.setEventTimestamp(System.currentTimeMillis());

		final TraceabilityLogDTO log2 = new TraceabilityLogDTO();
		log2.setCarId(4);
		log2.setBrand("Chevrolet");
		log2.setEventType("MAINTENANCE_COMPLETED");
		log2.setEventDetails("Maintenance completed.");
		log2.setEventTimestamp(System.currentTimeMillis());

		final String json1 = objectMapper.writeValueAsString(log1);
		final String json2 = objectMapper.writeValueAsString(log2);

		mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json1))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json2))
				.andExpect(status().isCreated());

		// Get all logs
		final MvcResult result = mockMvc.perform(get("/traceability-logs"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode logs = objectMapper.readTree(responseBody);

		assertEquals(2, logs.size());
	}

	@Test
	public void testSearchTraceabilityLogs() throws Exception {
		// Create a log
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(5);
		dto.setBrand("BMW");
		dto.setEventType("INSPECTION_COMPLETED");
		dto.setEventDetails("Quality inspection completed.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated());

		// Search by brand
		final Map<String, String> params = new HashMap<>();
		params.put("brand", "BMW");
		final MvcResult result = mockMvc.perform(get("/traceability-logs/search")
				.param("brand", "BMW"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode logs = objectMapper.readTree(responseBody);

		assertEquals(1, logs.size());
		assertEquals("BMW", logs.get(0).get("brand").asText());
	}

	@Test
	public void testUpdateTraceabilityLog() throws Exception {
		// Create a log
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(6);
		dto.setBrand("Audi");
		dto.setEventType("INSPECTION_COMPLETED");
		dto.setEventDetails("Quality inspection completed.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode log = objectMapper.readTree(responseBody);
		final int logId = log.get("id").asInt();

		// Update the log
		final TraceabilityLogDTO updatedDTO = new TraceabilityLogDTO();
		updatedDTO.setCarId(6);
		updatedDTO.setBrand("Audi");
		updatedDTO.setEventType("MAINTENANCE_COMPLETED");
		updatedDTO.setEventDetails("Maintenance completed.");
		updatedDTO.setEventTimestamp(System.currentTimeMillis());

		final String updatedJson = objectMapper.writeValueAsString(updatedDTO);
		final MvcResult updateResult = mockMvc.perform(put("/traceability-logs/" + logId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updatedJson))
				.andExpect(status().isOk())
				.andReturn();

		final String updateResponseBody = updateResult.getResponse().getContentAsString();
		final JsonNode updatedLog = objectMapper.readTree(updateResponseBody);

		assertEquals("Audi", updatedLog.get("brand").asText());
		assertEquals("MAINTENANCE_COMPLETED", updatedLog.get("eventType").asText());
	}

	@Test
	public void testDeleteTraceabilityLog() throws Exception {
		// Create a log
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(7);
		dto.setBrand("Tesla");
		dto.setEventType("INSPECTION_COMPLETED");
		dto.setEventDetails("Quality inspection completed.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode log = objectMapper.readTree(responseBody);
		final int logId = log.get("id").asInt();

		// Delete the log
		mockMvc.perform(delete("/traceability-logs/" + logId))
				.andExpect(status().isNoContent());

		// Verify it's deleted
		mockMvc.perform(get("/traceability-logs/" + logId))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testLogEvent() throws Exception {
		// First create a log with carId=8
		final TraceabilityLogDTO dto = new TraceabilityLogDTO();
		dto.setCarId(8);
		dto.setBrand("Mercedes");
		dto.setEventType("INSPECTION_COMPLETED");
		dto.setEventDetails("Quality inspection completed successfully.");
		dto.setEventTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		mockMvc.perform(post("/traceability-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated());

		// Now search for it using the log endpoint
		final MvcResult result = mockMvc.perform(post("/traceability-logs/log")
				.param("car-id", "8")
				.param("brand-name", "Mercedes")
				.param("event-type", "INSPECTION_COMPLETED"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode log = objectMapper.readTree(responseBody);

		assertEquals(8, log.get("carId").asInt());
		assertEquals("Mercedes", log.get("brand").asText());
		assertEquals("INSPECTION_COMPLETED", log.get("eventType").asText());
		assertNotNull(log.get("eventDetails"));
	}
}
