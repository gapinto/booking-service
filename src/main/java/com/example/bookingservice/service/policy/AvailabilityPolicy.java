package com.example.bookingservice.service.policy;

import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BlockRepository;
import com.example.bookingservice.repository.BookingRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class AvailabilityPolicy {

	private final BookingRepository bookingRepository;
	private final BlockRepository blockRepository;
	private final MessageSource messageSource;

	public AvailabilityPolicy(BookingRepository bookingRepository, BlockRepository blockRepository, MessageSource messageSource) {
		this.bookingRepository = bookingRepository;
		this.blockRepository = blockRepository;
		this.messageSource = messageSource;
	}

	public void ensureBookingDatesAvailable(String propertyId,
	                                       LocalDate startDate,
	                                       LocalDate endDate,
	                                       UUID excludedBookingId) {
		boolean overlapsBlock = !blockRepository
			.findOverlappingBlocks(propertyId, startDate, endDate)
			.isEmpty();
		if (overlapsBlock) {
			String msg = messageSource.getMessage(
				"error.booking.overlap.block",
				new Object[]{startDate, endDate, propertyId},
				LocaleContextHolder.getLocale()
			);
			throw new IllegalStateException(msg);
		}
		boolean overlapsBooking = bookingRepository
			.findOverlappingActiveBookings(propertyId, BookingStatus.CANCELED, startDate, endDate)
			.stream()
			.anyMatch(b -> excludedBookingId == null || !b.getId().equals(excludedBookingId));
		if (overlapsBooking) {
			String msg = messageSource.getMessage(
				"error.booking.overlap.booking",
				new Object[]{startDate, endDate, propertyId},
				LocaleContextHolder.getLocale()
			);
			throw new IllegalStateException(msg);
		}
	}

	public void ensureBlockDatesAllowed(String propertyId,
	                                    LocalDate startDate,
	                                    LocalDate endDate,
	                                    java.util.UUID excludedBlockId) {
		boolean overlapsActiveBooking = !bookingRepository
			.findOverlappingActiveBookings(propertyId, BookingStatus.CANCELED, startDate, endDate)
			.isEmpty();
		if (overlapsActiveBooking) {
			String msg = messageSource.getMessage(
				"error.block.overlap.booking",
				new Object[]{startDate, endDate, propertyId},
				LocaleContextHolder.getLocale()
			);
			throw new IllegalStateException(msg);
		}
		boolean overlapsAnotherBlock = blockRepository
			.findOverlappingBlocks(propertyId, startDate, endDate)
			.stream()
			.anyMatch(b -> excludedBlockId == null || !b.getId().equals(excludedBlockId));
		if (overlapsAnotherBlock) {
			String msg = messageSource.getMessage(
				"error.block.overlap.block",
				new Object[]{startDate, endDate, propertyId},
				LocaleContextHolder.getLocale()
			);
			throw new IllegalStateException(msg);
		}
	}
}


