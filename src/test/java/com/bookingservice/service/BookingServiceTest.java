package com.bookingservice.service;

import com.bookingservice.model.Allocation;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.model.AllocationType;
import com.bookingservice.model.Booking;
import com.bookingservice.model.BookingStatus;
import com.bookingservice.repository.AllocationRepository;
import com.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("BookingService - Given/When/Then")
class BookingServiceTest {

	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private AllocationRepository allocationRepository;
	@Autowired
	private BookingService bookingService;

	private static final String PROPERTY = "property-123";

	@BeforeEach
	void clean() {
		bookingRepository.deleteAll();
		allocationRepository.deleteAll();
	}

	@Test
	@DisplayName("Given no overlaps When creating a booking Then it is created as ACTIVE")
	void givenNoOverlaps_whenCreateBooking_thenBookingCreated() {
		// Given
		LocalDate start = LocalDate.now().plusDays(1);
		LocalDate end = start.plusDays(3);

		// When
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("John Doe");
		seed.setGuestEmail("john@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		Booking booking = bookingService.createBooking(seed);

		// Then
		assertNotNull(booking.getId());
		assertEquals(PROPERTY, booking.getPropertyId());
		assertEquals(BookingStatus.ACTIVE, booking.getStatus());
		assertEquals(1, bookingRepository.count());
	}

	@Test
	@DisplayName("Given an active booking When creating an overlapping booking Then it fails with conflict")
	void givenActiveBookingOverlap_whenCreateBooking_thenIllegalState() {
		// Given
		LocalDate start = LocalDate.now().plusDays(1);
		LocalDate end = start.plusDays(3);
		Booking seed = new Booking();
		seed.setPropertyId(PROPERTY);
		seed.setGuestName("John Doe");
		seed.setGuestEmail("john@example.com");
		seed.setStartDate(start);
		seed.setEndDate(end);
		bookingService.createBooking(seed);

		// When / Then
		LocalDate overlappingStart = start.plusDays(2);
		LocalDate overlappingEnd = overlappingStart.plusDays(2);

		assertThrows(IllegalStateException.class, () ->
			{
				Booking b = new Booking();
				b.setPropertyId(PROPERTY);
				b.setGuestName("Jane Roe");
				b.setGuestEmail("jane@example.com");
				b.setStartDate(overlappingStart);
				b.setEndDate(overlappingEnd);
				bookingService.createBooking(b);
			}
		);
	}

	@Test
	@DisplayName("Given a block When creating an overlapping booking Then it fails with conflict")
	void givenBlockOverlap_whenCreateBooking_thenIllegalState() {
		// Given
		LocalDate blockStart = LocalDate.now().plusDays(5);
		LocalDate blockEnd = blockStart.plusDays(4);
		Allocation blockAllocation = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(blockStart)
			.endDate(blockEnd)
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(blockAllocation);

		// When / Then
		LocalDate start = blockStart.plusDays(1);
		LocalDate end = start.plusDays(2);

		assertThrows(IllegalStateException.class, () ->
			{
				Booking b = new Booking();
				b.setPropertyId(PROPERTY);
				b.setGuestName("John Doe");
				b.setGuestEmail("john@example.com");
				b.setStartDate(start);
				b.setEndDate(end);
				bookingService.createBooking(b);
			}
		);
	}

	@Test
	@DisplayName("Given invalid dates When creating a booking Then it fails with bad request")
	void givenInvalidDates_whenCreateBooking_thenIllegalArgument() {
		// Given
		LocalDate start = LocalDate.now().plusDays(5);
		LocalDate end = start.minusDays(1);

		// When / Then
		assertThrows(IllegalArgumentException.class, () ->
		{
			Booking b = new Booking();
			b.setPropertyId(PROPERTY);
			b.setGuestName("John Doe");
			b.setGuestEmail("john@example.com");
			b.setStartDate(start);
			b.setEndDate(end);
			bookingService.createBooking(b);
		}
		);
	}

	@Test
	@DisplayName("Given a canceled booking When creating an overlapping booking Then it succeeds (canceled does not count)")
	void givenCanceledBooking_whenCreateOverlappingBooking_thenAllowed() {
		// Given
		LocalDate start = LocalDate.now().plusDays(2);
		LocalDate end = start.plusDays(2);
		Booking b1 = new Booking();
		b1.setPropertyId(PROPERTY);
		b1.setGuestName("Alice");
		b1.setGuestEmail("alice@example.com");
		b1.setStartDate(start);
		b1.setEndDate(end);
		Booking created = bookingService.createBooking(b1);
		bookingService.cancelBooking(created.getId());

		// When
		Booking b2 = new Booking();
		b2.setPropertyId(PROPERTY);
		b2.setGuestName("Bob");
		b2.setGuestEmail("bob@example.com");
		b2.setStartDate(start.plusDays(1));
		b2.setEndDate(end.plusDays(1));
		Booking created2 = bookingService.createBooking(b2);

		// Then
		assertNotNull(created2.getId());
		assertEquals(2, bookingRepository.count());
	}

	@Test
	@DisplayName("Given a canceled booking and a new conflicting block When rebooking Then it fails with conflict")
	void givenCanceledBookingAndBlock_whenRebook_thenConflict() {
		// Given
		LocalDate start = LocalDate.now().plusDays(7);
		LocalDate end = start.plusDays(3);
		Booking b = new Booking();
		b.setPropertyId(PROPERTY);
		b.setGuestName("Carol");
		b.setGuestEmail("carol@example.com");
		b.setStartDate(start);
		b.setEndDate(end);
		Booking created = bookingService.createBooking(b);
		bookingService.cancelBooking(created.getId());

		Allocation blockAllocation = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(blockAllocation);

		// When / Then
		assertThrows(IllegalStateException.class, () -> bookingService.rebookBooking(created.getId()));
	}
}


