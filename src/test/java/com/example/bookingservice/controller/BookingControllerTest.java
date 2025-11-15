package com.example.bookingservice.controller;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
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
@DisplayName("BookingController - Given/When/Then")
class BookingControllerTest {

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

	private static final String PROPERTY = "property-abc";

	@BeforeEach
	void resetDb() {
		bookingRepository.deleteAll();
		blockRepository.deleteAll();
	}
	@Test
	@DisplayName("Given a valid request When creating a booking Then 201 and retrieval returns 200")
	void givenValidRequest_whenCreate_then201AndGet200() throws Exception {
		// Given
		var payload = new CreateReq(PROPERTY, "John", "john@example.com",
			LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

		// When
		var result = mockMvc.perform(post("/api/bookings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.propertyId", is(PROPERTY)))
			.andExpect(jsonPath("$.guestName", is("John")))
			.andExpect(jsonPath("$.status", is(BookingStatus.ACTIVE.name())))
			.andReturn();

		// Then
		var response = objectMapper.readTree(result.getResponse().getContentAsString());
		var id = UUID.fromString(response.get("id").asText());

		mockMvc.perform(get("/api/bookings/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(id.toString())))
			.andExpect(jsonPath("$.propertyId", is(PROPERTY)));
	}

	@Test
	@DisplayName("Given an overlapping range When creating a booking Then 409 Conflict")
	void givenOverlap_whenCreate_then409() throws Exception {
		// Given
		LocalDate start = LocalDate.now().plusDays(5);
		LocalDate end = start.plusDays(2);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("Jane");
		seed.setGuestEmail("jane@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		bookingService.createBooking(seed);

		var payload = new CreateReq(PROPERTY, "John", "john@example.com",
			start.plusDays(1), end.plusDays(1));

		// When / Then
		mockMvc.perform(post("/api/bookings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error", is("CONFLICT")));
	}

	@Test
	@DisplayName("Given a canceled booking When rebooking Then 200 and status ACTIVE")
	void givenCanceledBooking_whenRebook_then200() throws Exception {
		// Given
		LocalDate start = LocalDate.now().plusDays(10);
		LocalDate end = start.plusDays(2);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("Jane");
		seed.setGuestEmail("jane@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		Booking booking = bookingService.createBooking(seed);

		mockMvc.perform(post("/api/bookings/{id}/cancel", booking.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is(BookingStatus.CANCELED.name())));

		mockMvc.perform(post("/api/bookings/{id}/rebook", booking.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is(BookingStatus.ACTIVE.name())));
	}

	@Test
	@DisplayName("Given a valid update request When updating Then 200 and delete returns 204")
	void givenValidUpdate_whenUpdate_then200AndDelete204() throws Exception {
		// Given
		LocalDate start = LocalDate.now().plusDays(20);
		LocalDate end = start.plusDays(2);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("Jane");
		seed.setGuestEmail("jane@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		Booking booking = bookingService.createBooking(seed);

		var update = new UpdateReq("Jane Doe", "jane.doe@example.com",
			start.plusDays(1), end.plusDays(1));

		mockMvc.perform(put("/api/bookings/{id}", booking.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.guestName", is("Jane Doe")))
			.andExpect(jsonPath("$.guestEmail", is("jane.doe@example.com")));

		mockMvc.perform(delete("/api/bookings/{id}", booking.getId()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/bookings/{id}", booking.getId()))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Given a canceled booking and a new conflicting block When rebooking Then 409")
	void givenCanceledBookingAndBlock_whenRebook_then409() throws Exception {
		// Given
		LocalDate start = LocalDate.now().plusDays(15);
		LocalDate end = start.plusDays(2);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("Zed");
		seed.setGuestEmail("zed@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		var created = bookingService.createBooking(seed);

		mockMvc.perform(post("/api/bookings/{id}/cancel", created.getId()))
			.andExpect(status().isOk());

		var blockPayload = new BlockControllerTest.BlockReq(PROPERTY, start, end);
		mockMvc.perform(post("/api/blocks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(blockPayload)))
			.andExpect(status().isCreated());

		// When / Then
		mockMvc.perform(post("/api/bookings/{id}/rebook", created.getId()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error", is("CONFLICT")));
	}

	record CreateReq(String propertyId, String guestName, String guestEmail, LocalDate startDate, LocalDate endDate) {}
	record UpdateReq(String guestName, String guestEmail, LocalDate startDate, LocalDate endDate) {}
}


