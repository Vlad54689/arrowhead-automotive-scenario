package ai.aitia.demo.quality_inspection_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import ai.aitia.demo.quality_inspection_service.dto.QualityInspectionDTO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class QualityInspectionServiceApplicationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() throws Exception {
		// Clean up any existing inspections
		final String getAllResponse = mockMvc.perform(get("/quality-inspections"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		final JsonNode inspections = objectMapper.readTree(getAllResponse);
		for (JsonNode inspection : inspections) {
			if (inspection.has("id")) {
				mockMvc.perform(delete("/quality-inspections/" + inspection.get("id").asInt()))
						.andExpect(status().isNoContent());
			}
		}
	}

	@Test
	public void testCreateQualityInspection() throws Exception {
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setCarId(1);
		dto.setBrand("Toyota");
		dto.setColor("Silver");
		dto.setDefectDetected(false);
		dto.setDefectType(null);
		dto.setInspectionResult("NO_DEFECT");
		dto.setInspectionTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspection = objectMapper.readTree(responseBody);

		assertNotNull(inspection.get("id"));
		assertEquals(1, inspection.get("carId").asInt());
		assertEquals("Toyota", inspection.get("brand").asText());
		assertEquals("Silver", inspection.get("color").asText());
		assertEquals("NO_DEFECT", inspection.get("inspectionResult").asText());
	}

	@Test
	public void testGetQualityInspectionById() throws Exception {
		// First create an inspection
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setCarId(2);
		dto.setBrand("Honda");
		dto.setColor("Blue");
		dto.setDefectDetected(true);
		dto.setDefectType("SCRATCH");
		dto.setInspectionResult("DEFECT_DETECTED");
		dto.setInspectionTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspection = objectMapper.readTree(responseBody);
		final int inspectionId = inspection.get("id").asInt();

		// Then get it by ID
		final MvcResult getResult = mockMvc.perform(get("/quality-inspections/" + inspectionId))
				.andExpect(status().isOk())
				.andReturn();

		final String getResultBody = getResult.getResponse().getContentAsString();
		final JsonNode retrievedInspection = objectMapper.readTree(getResultBody);

		assertEquals(2, retrievedInspection.get("carId").asInt());
		assertEquals("Honda", retrievedInspection.get("brand").asText());
		assertEquals("SCRATCH", retrievedInspection.get("defectType").asText());
		assertEquals("DEFECT_DETECTED", retrievedInspection.get("inspectionResult").asText());
	}

	@Test
	public void testGetAllQualityInspections() throws Exception {
		// Create two inspections
		final QualityInspectionDTO inspection1 = new QualityInspectionDTO();
		inspection1.setCarId(3);
		inspection1.setBrand("Ford");
		inspection1.setColor("Red");
		inspection1.setDefectDetected(false);
		inspection1.setInspectionResult("NO_DEFECT");
		inspection1.setInspectionTimestamp(System.currentTimeMillis());

		final QualityInspectionDTO inspection2 = new QualityInspectionDTO();
		inspection2.setCarId(4);
		inspection2.setBrand("Chevrolet");
		inspection2.setColor("Yellow");
		inspection2.setDefectDetected(true);
		inspection2.setDefectType("DENT");
		inspection2.setInspectionResult("DEFECT_DETECTED");
		inspection2.setInspectionTimestamp(System.currentTimeMillis());

		final String json1 = objectMapper.writeValueAsString(inspection1);
		final String json2 = objectMapper.writeValueAsString(inspection2);

		mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json1))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json2))
				.andExpect(status().isCreated());

		// Get all inspections
		final MvcResult result = mockMvc.perform(get("/quality-inspections"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspections = objectMapper.readTree(responseBody);

		assertEquals(2, inspections.size());
	}

	@Test
	public void testSearchQualityInspections() throws Exception {
		// Create an inspection
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setCarId(5);
		dto.setBrand("BMW");
		dto.setColor("Black");
		dto.setDefectDetected(false);
		dto.setInspectionResult("NO_DEFECT");
		dto.setInspectionTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated());

		// Search by brand
		final Map<String, String> params = new HashMap<>();
		params.put("brand", "BMW");
		final MvcResult result = mockMvc.perform(get("/quality-inspections/search")
				.param("brand", "BMW"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspections = objectMapper.readTree(responseBody);

		assertEquals(1, inspections.size());
		assertEquals("BMW", inspections.get(0).get("brand").asText());
	}

	@Test
	public void testUpdateQualityInspection() throws Exception {
		// Create an inspection
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setCarId(6);
		dto.setBrand("Audi");
		dto.setColor("White");
		dto.setDefectDetected(false);
		dto.setInspectionResult("NO_DEFECT");
		dto.setInspectionTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspection = objectMapper.readTree(responseBody);
		final int inspectionId = inspection.get("id").asInt();

		// Update the inspection
		final QualityInspectionDTO updatedDTO = new QualityInspectionDTO();
		updatedDTO.setCarId(6);
		updatedDTO.setBrand("Audi");
		updatedDTO.setColor("Black");
		updatedDTO.setDefectDetected(true);
		updatedDTO.setDefectType("PAINT_DAMAGE");
		updatedDTO.setInspectionResult("DEFECT_DETECTED");
		updatedDTO.setInspectionTimestamp(System.currentTimeMillis());

		final String updatedJson = objectMapper.writeValueAsString(updatedDTO);
		final MvcResult updateResult = mockMvc.perform(put("/quality-inspections/" + inspectionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updatedJson))
				.andExpect(status().isOk())
				.andReturn();

		final String updateResponseBody = updateResult.getResponse().getContentAsString();
		final JsonNode updatedInspection = objectMapper.readTree(updateResponseBody);

		assertEquals("Audi", updatedInspection.get("brand").asText());
		assertEquals("Black", updatedInspection.get("color").asText());
		assertEquals("PAINT_DAMAGE", updatedInspection.get("defectType").asText());
		assertEquals("DEFECT_DETECTED", updatedInspection.get("inspectionResult").asText());
	}

	@Test
	public void testDeleteQualityInspection() throws Exception {
		// Create an inspection
		final QualityInspectionDTO dto = new QualityInspectionDTO();
		dto.setCarId(7);
		dto.setBrand("Tesla");
		dto.setColor("Gray");
		dto.setDefectDetected(false);
		dto.setInspectionResult("NO_DEFECT");
		dto.setInspectionTimestamp(System.currentTimeMillis());

		final String json = objectMapper.writeValueAsString(dto);
		final MvcResult result = mockMvc.perform(post("/quality-inspections")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspection = objectMapper.readTree(responseBody);
		final int inspectionId = inspection.get("id").asInt();

		// Delete the inspection
		mockMvc.perform(delete("/quality-inspections/" + inspectionId))
				.andExpect(status().isNoContent());

		// Verify it's deleted
		mockMvc.perform(get("/quality-inspections/" + inspectionId))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testInspectCar() throws Exception {
		final Map<String, String> params = new HashMap<>();
		params.put("carId", "8");
		params.put("brand", "Mercedes");
		params.put("color", "Silver");
		params.put("inspect", "true");

		final MvcResult result = mockMvc.perform(post("/quality-inspections/inspect")
				.param("carId", "8")
				.param("brand", "Mercedes")
				.param("color", "Silver")
				.param("inspect", "true"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode inspection = objectMapper.readTree(responseBody);

		assertEquals(8, inspection.get("carId").asInt());
		assertEquals("Mercedes", inspection.get("brand").asText());
		assertEquals("Silver", inspection.get("color").asText());
		assertEquals("DEFECT_DETECTED", inspection.get("inspectionResult").asText());
		assertNotNull(inspection.get("defectType"));
	}
}
