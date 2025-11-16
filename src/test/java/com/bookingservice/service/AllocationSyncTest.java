package com.bookingservice.service;

import com.bookingservice.model.Allocation;
import com.bookingservice.model.AllocationType;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.model.Booking;
import com.bookingservice.model.BookingStatus;
import com.bookingservice.repository.AllocationRepository;
import com.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Allocation Sync - Given/When/Then")
class AllocationSyncTest {

	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private AllocationRepository allocationRepository;
	@Autowired
	private BookingService bookingService;

	private static final String PROPERTY = "property-sync-test";

	@BeforeEach
	void resetDb() {
		bookingRepository.deleteAll();
		allocationRepository.deleteAll();
	}

	@Test
	@DisplayName("Given a booking created When checking allocations Then allocation exists")
	void givenBookingCreated_whenCheckAllocations_thenAllocationExists() {
		// Given
		Booking booking = new Booking();
		booking.setPropertyId(PROPERTY);
		booking.setGuestName("John");
		booking.setGuestEmail("john@example.com");
		booking.setStartDate(LocalDate.now().plusDays(1));
		booking.setEndDate(LocalDate.now().plusDays(3));

		// When
		Booking created = bookingService.createBooking(booking);

		// Then
		List<Allocation> allocations = allocationRepository.findAll();
		assertEquals(1, allocations.size());
		
		Allocation allocation = allocations.get(0);
		assertEquals(created.getId(), allocation.getEntityId());
		assertEquals(AllocationType.BOOKING, allocation.getType());
		assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
		assertEquals(PROPERTY, allocation.getPropertyId());
		assertEquals(created.getStartDate(), allocation.getStartDate());
		assertEquals(created.getEndDate(), allocation.getEndDate());
	}

	@Test
	@DisplayName("Given a booking canceled When checking allocations Then allocation status is CANCELED")
	void givenBookingCanceled_whenCheckAllocations_thenAllocationCanceled() {
		// Given
		Booking booking = new Booking();
		booking.setPropertyId(PROPERTY);
		booking.setGuestName("Jane");
		booking.setGuestEmail("jane@example.com");
		booking.setStartDate(LocalDate.now().plusDays(5));
		booking.setEndDate(LocalDate.now().plusDays(7));
		Booking created = bookingService.createBooking(booking);

		// When
		bookingService.cancelBooking(created.getId());

		// Then
		List<Allocation> allocations = allocationRepository.findAll();
		assertEquals(1, allocations.size());
		assertEquals(AllocationStatus.CANCELED, allocations.get(0).getStatus());
	}

	@Test
	@DisplayName("Given a booking updated When checking allocations Then allocation dates updated")
	void givenBookingUpdated_whenCheckAllocations_thenAllocationDatesUpdated() {
		// Given
		Booking booking = new Booking();
		booking.setPropertyId(PROPERTY);
		booking.setGuestName("Bob");
		booking.setGuestEmail("bob@example.com");
		booking.setStartDate(LocalDate.now().plusDays(10));
		booking.setEndDate(LocalDate.now().plusDays(12));
		Booking created = bookingService.createBooking(booking);

		// When
		LocalDate newStart = LocalDate.now().plusDays(15);
		LocalDate newEnd = LocalDate.now().plusDays(17);
		var updateReq = new com.bookingservice.api.dto.BookingDtos.UpdateRequest(
			"Bob Updated", "bob@example.com", newStart, newEnd
		);
		bookingService.updateBooking(created.getId(), updateReq);

		// Then
		List<Allocation> allocations = allocationRepository.findAll();
		assertEquals(1, allocations.size());
		assertEquals(newStart, allocations.get(0).getStartDate());
		assertEquals(newEnd, allocations.get(0).getEndDate());
	}

	@Test
	@DisplayName("Given a booking deleted When checking allocations Then allocation is deleted")
	void givenBookingDeleted_whenCheckAllocations_thenAllocationDeleted() {
		// Given
		Booking booking = new Booking();
		booking.setPropertyId(PROPERTY);
		booking.setGuestName("Alice");
		booking.setGuestEmail("alice@example.com");
		booking.setStartDate(LocalDate.now().plusDays(20));
		booking.setEndDate(LocalDate.now().plusDays(22));
		Booking created = bookingService.createBooking(booking);

		// When
		bookingService.deleteBooking(created.getId());

		// Then
		List<Allocation> allocations = allocationRepository.findAll();
		assertTrue(allocations.isEmpty());
	}
}


