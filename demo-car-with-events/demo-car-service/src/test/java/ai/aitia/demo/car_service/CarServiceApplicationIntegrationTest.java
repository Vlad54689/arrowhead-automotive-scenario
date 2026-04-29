package ai.aitia.demo.car_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import ai.aitia.demo.car_service.dto.CarDTO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class CarServiceApplicationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private int createdCarId;

	@BeforeEach
	public void setUp() throws Exception {
		// Clean up any existing cars first
		final String getAllResponse = mockMvc.perform(get("/cars"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		final JsonNode cars = objectMapper.readTree(getAllResponse);
		for (JsonNode car : cars) {
			if (car.has("id")) {
				mockMvc.perform("/cars/" + car.get("id").asInt())
						.andExpect(status().isOk());
			}
		}
	}

	@Test
	public void testCreateCar() throws Exception {
		final CarDTO carDTO = new CarDTO();
		carDTO.setBrand("Toyota");
		carDTO.setModel("Camry");
		carDTO.setColor("Silver");
		carDTO.setYear(2023);

		final String json = objectMapper.writeValueAsString(carDTO);
		final MvcResult result = mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode car = objectMapper.readTree(responseBody);

		assertNotNull(car.get("id"));
		assertEquals("Toyota", car.get("brand").asText());
		assertEquals("Camry", car.get("model").asText());
		assertEquals("Silver", car.get("color").asText());
		assertEquals(2023, car.get("year").asInt());

		createdCarId = car.get("id").asInt();
	}

	@Test
	public void testGetCarById() throws Exception {
		// First create a car
		final CarDTO carDTO = new CarDTO();
		carDTO.setBrand("Honda");
		carDTO.setModel("Civic");
		carDTO.setColor("Blue");
		carDTO.setYear(2022);

		final String json = objectMapper.writeValueAsString(carDTO);
		final MvcResult result = mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode car = objectMapper.readTree(responseBody);
		final int carId = car.get("id").asInt();

		// Then get it by ID
		final MvcResult getResult = mockMvc.perform(get("/cars/" + carId))
				.andExpect(status().isOk())
				.andReturn();

		final String getResultBody = getResult.getResponse().getContentAsString();
		final JsonNode retrievedCar = objectMapper.readTree(getResultBody);

		assertEquals("Honda", retrievedCar.get("brand").asText());
		assertEquals("Civic", retrievedCar.get("model").asText());
		assertEquals("Blue", retrievedCar.get("color").asText());
		assertEquals(2022, retrievedCar.get("year").asInt());
	}

	@Test
	public void testGetAllCars() throws Exception {
		// Create two cars
		final CarDTO car1 = new CarDTO();
		car1.setBrand("Ford");
		car1.setModel("Mustang");
		car1.setColor("Red");
		car1.setYear(2021);

		final CarDTO car2 = new CarDTO();
		car2.setBrand("Chevrolet");
		car2.setModel("Corvette");
		car2.setColor("Yellow");
		car2.setYear(2020);

		final String json1 = objectMapper.writeValueAsString(car1);
		final String json2 = objectMapper.writeValueAsString(car2);

		mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json1))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json2))
				.andExpect(status().isCreated());

		// Get all cars
		final MvcResult result = mockMvc.perform(get("/cars"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode cars = objectMapper.readTree(responseBody);

		assertEquals(2, cars.size());
	}

	@Test
	public void testSearchCars() throws Exception {
		// Create a car
		final CarDTO carDTO = new CarDTO();
		carDTO.setBrand("BMW");
		carDTO.setModel("X5");
		carDTO.setColor("Black");
		carDTO.setYear(2023);

		final String json = objectMapper.writeValueAsString(carDTO);
		mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated());

		// Search by brand
		final Map<String, String> params = new HashMap<>();
		params.put("brand", "BMW");
		final MvcResult result = mockMvc.perform(get("/cars/search")
				.param("brand", "BMW"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode cars = objectMapper.readTree(responseBody);

		assertEquals(1, cars.size());
		assertEquals("BMW", cars.get(0).get("brand").asText());
	}

	@Test
	public void testUpdateCar() throws Exception {
		// Create a car
		final CarDTO carDTO = new CarDTO();
		carDTO.setBrand("Audi");
		carDTO.setModel("A4");
		carDTO.setColor("White");
		carDTO.setYear(2022);

		final String json = objectMapper.writeValueAsString(carDTO);
		final MvcResult result = mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode car = objectMapper.readTree(responseBody);
		final int carId = car.get("id").asInt();

		// Update the car
		final CarDTO updatedDTO = new CarDTO();
		updatedDTO.setBrand("Audi");
		updatedDTO.setModel("A6");
		updatedDTO.setColor("Black");
		updatedDTO.setYear(2023);

		final String updatedJson = objectMapper.writeValueAsString(updatedDTO);
		final MvcResult updateResult = mockMvc.perform(post("/cars/" + carId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updatedJson))
				.andExpect(status().isOk())
				.andReturn();

		final String updateResponseBody = updateResult.getResponse().getContentAsString();
		final JsonNode updatedCar = objectMapper.readTree(updateResponseBody);

		assertEquals("Audi", updatedCar.get("brand").asText());
		assertEquals("A6", updatedCar.get("model").asText());
		assertEquals("Black", updatedCar.get("color").asText());
		assertEquals(2023, updatedCar.get("year").asInt());
	}

	@Test
	public void testDeleteCar() throws Exception {
		// Create a car
		final CarDTO carDTO = new CarDTO();
		carDTO.setBrand("Tesla");
		carDTO.setModel("Model S");
		carDTO.setColor("Gray");
		carDTO.setYear(2023);

		final String json = objectMapper.writeValueAsString(carDTO);
		final MvcResult result = mockMvc.perform(post("/cars")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode car = objectMapper.readTree(responseBody);
		final int carId = car.get("id").asInt();

		// Delete the car
		mockMvc.perform(post("/cars/delete/" + carId))
				.andExpect(status().isNoContent());

		// Verify it's deleted
		mockMvc.perform(get("/cars/" + carId))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testGetCarsByBrand() throws Exception {
		// Create multiple cars with same brand
		for (int i = 0; i < 3; i++) {
			final CarDTO carDTO = new CarDTO();
			carDTO.setBrand("Mercedes");
			carDTO.setModel("C-Class");
			carDTO.setColor("Silver");
			carDTO.setYear(2022);

			final String json = objectMapper.writeValueAsString(carDTO);
			mockMvc.perform(post("/cars")
					.contentType(MediaType.APPLICATION_JSON)
					.content(json))
					.andExpect(status().isCreated());
		}

		// Get cars by brand
		final Map<String, String> params = new HashMap<>();
		params.put("brand", "Mercedes");
		final MvcResult result = mockMvc.perform(get("/cars/search")
				.param("brand", "Mercedes"))
				.andExpect(status().isOk())
				.andReturn();

		final String responseBody = result.getResponse().getContentAsString();
		final JsonNode cars = objectMapper.readTree(responseBody);

		assertEquals(3, cars.size());
	}
}
