package com.example.bookingservice.service.policy;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.model.Block;
import com.example.bookingservice.repository.BlockRepository;
import com.example.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DisplayName("AvailabilityPolicy - Given/When/Then")
class AvailabilityPolicyTest {

	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private BlockRepository blockRepository;
	@Autowired
	private AvailabilityPolicy availabilityPolicy;

	private static final String PROPERTY = "prop-policy";

	@BeforeEach
	void setup() {
		bookingRepository.deleteAll();
		blockRepository.deleteAll();
	}

	@Test
	@DisplayName("Given no overlaps When checking booking availability Then it allows")
	void givenNoOverlaps_whenCheckBooking_thenAllows() {
		assertDoesNotThrow(() -> availabilityPolicy.ensureBookingDatesAvailable(
			PROPERTY, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null
		));
	}

	@Test
	@DisplayName("Given an active booking When checking overlapping booking Then it blocks")
	void givenActiveBooking_whenCheckOverlappingBooking_thenBlocks() {
		var start = LocalDate.now().plusDays(2);
		var end = start.plusDays(2);
		Booking b = new Booking();
		b.setPropertyId(PROPERTY);
		b.setGuestName("A");
		b.setGuestEmail("a@a.com");
		b.setStartDate(start);
		b.setEndDate(end);
		b.setStatus(BookingStatus.ACTIVE);
		bookingRepository.save(b);

		assertThrows(IllegalStateException.class, () ->
			availabilityPolicy.ensureBookingDatesAvailable(PROPERTY, start.plusDays(1), end.plusDays(1), null)
		);
	}

	@Test
	@DisplayName("Given an existing booking When checking availability ignoring itself Then it allows")
	void givenExistingBooking_whenCheckIgnoringSelf_thenAllows() {
		var start = LocalDate.now().plusDays(2);
		var end = start.plusDays(2);
		Booking b = new Booking();
		b.setPropertyId(PROPERTY);
		b.setGuestName("A");
		b.setGuestEmail("a@a.com");
		b.setStartDate(start);
		b.setEndDate(end);
		b.setStatus(BookingStatus.ACTIVE);
		var saved = bookingRepository.save(b);

		assertDoesNotThrow(() ->
			availabilityPolicy.ensureBookingDatesAvailable(PROPERTY, start, end, saved.getId())
		);
	}

	@Test
	@DisplayName("Given a block When creating an overlapping block Then it blocks")
	void givenExistingBlock_whenCreateOverlappingBlock_thenBlocks() {
		var start = LocalDate.now().plusDays(4);
		var end = start.plusDays(2);
		Block bl = new Block();
		bl.setPropertyId(PROPERTY);
		bl.setStartDate(start);
		bl.setEndDate(end);
		blockRepository.save(bl);

		assertThrows(IllegalStateException.class, () ->
			availabilityPolicy.ensureBlockDatesAllowed(PROPERTY, start.plusDays(1), end.plusDays(1), null)
		);
	}

	@Test
	@DisplayName("Given an existing block When checking availability ignoring itself Then it allows")
	void givenExistingBlock_whenCheckIgnoringSelf_thenAllows() {
		var start = LocalDate.now().plusDays(7);
		var end = start.plusDays(1);
		Block bl = new Block();
		bl.setPropertyId(PROPERTY);
		bl.setStartDate(start);
		bl.setEndDate(end);
		var saved = blockRepository.save(bl);

		// same dates should be allowed when excluding the same block id
		assertDoesNotThrow(() ->
			availabilityPolicy.ensureBlockDatesAllowed(PROPERTY, start, end, saved.getId())
		);
	}
}


