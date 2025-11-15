package com.example.bookingservice.controller;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.repository.BlockRepository;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BlockController - Given/When/Then")
class BlockControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private BookingService bookingService;
	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private BlockRepository blockRepository;

	private static final String PROPERTY = "property-xyz";

	@BeforeEach
	void resetDb() {
		bookingRepository.deleteAll();
		blockRepository.deleteAll();
	}
	@Test
	@DisplayName("Given a valid request When creating/updating/deleting a block Then 201/200/204")
	void givenValidRequest_whenCreateUpdateDeleteBlock_then201_200_204() throws Exception {
		// Given
		var start = LocalDate.now().plusDays(2);
		var end = start.plusDays(3);
		var payload = new BlockReq(PROPERTY, start, end);

		// When
		var result = mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.propertyId", is(PROPERTY)))
			.andReturn();

		// Then
		var response = objectMapper.readTree(result.getResponse().getContentAsString());
		var id = UUID.fromString(response.get("id").asText());

		var update = new BlockReq(PROPERTY, start.plusDays(1), end.plusDays(1));
		mockMvc.perform(put("/api/blocks/{id}", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startDate", is(update.startDate().toString())));

		mockMvc.perform(delete("/api/blocks/{id}", id))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Given an active booking When creating an overlapping block Then 409")
	void givenActiveBooking_whenCreateOverlappingBlock_then409() throws Exception {
		// Given
		LocalDate start = LocalDate.now().plusDays(10);
		LocalDate end = start.plusDays(2);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("John");
		seed.setGuestEmail("john@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		Booking booking = bookingService.createBooking(seed);
		var payload = new BlockReq(PROPERTY, start.plusDays(1), end.plusDays(1));

		// When / Then
		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error", is("CONFLICT")));
	}

	@Test
	@DisplayName("Given a block When creating an overlapping block Then 409")
	void givenBlock_whenCreateOverlappingBlock_then409() throws Exception {
		// Given
		var start = LocalDate.now().plusDays(3);
		var end = start.plusDays(3);
		var b1 = new BlockReq(PROPERTY, start, end);
		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(b1)))
			.andExpect(status().isCreated());

		var overlapping = new BlockReq(PROPERTY, start.plusDays(1), end.plusDays(1));
		// When / Then
		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(overlapping)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error", is("CONFLICT")));
	}

	@Test
	@DisplayName("Given two non-overlapping blocks When updating one to overlap Then 409")
	void givenTwoBlocks_whenUpdateToOverlap_then409() throws Exception {
		// Given
		var start = LocalDate.now().plusDays(8);
		var end = start.plusDays(2);
		// create first block
		var res1 = mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new BlockReq(PROPERTY, start, end))))
			.andExpect(status().isCreated())
			.andReturn();
		var id1 = UUID.fromString(objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asText());

		// create second block non-overlapping
		var res2 = mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new BlockReq(PROPERTY, end.plusDays(2), end.plusDays(4)))))
			.andExpect(status().isCreated())
			.andReturn();
		var id2 = UUID.fromString(objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asText());

		// update first to overlap second
		var update = new BlockReq(PROPERTY, end.plusDays(3), end.plusDays(5));
		// When / Then
		mockMvc.perform(put("/api/blocks/{id}", id1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error", is("CONFLICT")));
	}

	@Test
	@DisplayName("Given two non-overlapping blocks When creating both Then both return 201")
	void givenTwoNonOverlappingBlocks_whenCreateBoth_then201() throws Exception {
		// Given
		var start = LocalDate.now().plusDays(12);
		var end = start.plusDays(2);
		var block1 = new BlockReq(PROPERTY, start, end);
		var block2 = new BlockReq(PROPERTY, end.plusDays(2), end.plusDays(4));

		// When / Then
		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(block1)))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(block2)))
			.andExpect(status().isCreated());
	}

	record BlockReq(String propertyId, LocalDate startDate, LocalDate endDate) {}
}


