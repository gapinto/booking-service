package com.bookingservice.service;

import com.bookingservice.api.dto.BookingDtos;
import com.bookingservice.model.Allocation;
import com.bookingservice.model.AllocationType;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.model.Booking;
import com.bookingservice.model.BookingStatus;
import com.bookingservice.support.NotFoundException;
import com.bookingservice.repository.AllocationRepository;
import com.bookingservice.repository.BookingRepository;
import com.bookingservice.service.policy.AllocationPolicy;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.List;

@Service
public class BookingService {

	private final BookingRepository bookingRepository;
	private final AllocationRepository allocationRepository;
	private final AllocationPolicy allocationPolicy;
	private final MessageSource messageSource;

	public BookingService(BookingRepository bookingRepository, AllocationRepository allocationRepository, AllocationPolicy allocationPolicy, MessageSource messageSource) {
		this.bookingRepository = bookingRepository;
		this.allocationRepository = allocationRepository;
		this.allocationPolicy = allocationPolicy;
		this.messageSource = messageSource;
	}

	@Transactional
	public Booking createBooking(Booking booking) {
		if (booking == null) {
			throw new IllegalArgumentException(getMessage("error.validation.booking.required"));
		}
		if (booking.getId() == null) {
			booking.setId(java.util.UUID.randomUUID());
		}
		validateInputs(booking.getPropertyId(), booking.getGuestName(), booking.getGuestEmail(),
			booking.getStartDate(), booking.getEndDate());
		allocationPolicy.ensureDatesAvailableFor(booking, null);
		booking.setStatus(BookingStatus.ACTIVE);
		Booking saved = bookingRepository.save(booking);
		
		// Sync to Allocation table
		Allocation allocation = Allocation.builder()
			.id(java.util.UUID.randomUUID())
			.propertyId(saved.getPropertyId())
			.startDate(saved.getStartDate())
			.endDate(saved.getEndDate())
			.type(AllocationType.BOOKING)
			.status(AllocationStatus.ACTIVE)
			.entityId(saved.getId())
			.build();
		allocationRepository.save(allocation);
		
		return saved;
	}

	@Transactional(readOnly = true)
	public Booking getBooking(UUID id) {
		return bookingRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Booking not found: " + id));
	}

	@Transactional
	public Booking updateBooking(UUID id, BookingDtos.UpdateRequest update) {
		Booking existing = getBooking(id);
		// apply incoming values explicitly to ensure fields are updated
		existing.setGuestName(update.guestName());
		existing.setGuestEmail(update.guestEmail());
		existing.setStartDate(update.startDate());
		existing.setEndDate(update.endDate());
		// validate and check availability
		validateInputs(existing.getPropertyId(), existing.getGuestName(), existing.getGuestEmail(),
			existing.getStartDate(), existing.getEndDate());
		allocationPolicy.ensureDatesAvailableFor(existing, existing.getId());
		Booking saved = bookingRepository.save(existing);
		
		// Sync to Allocation table (1 query instead of N+1)
		allocationRepository.updateByEntityId(
			id,
			saved.getPropertyId(),
			saved.getStartDate(),
			saved.getEndDate()
		);
		
		return saved;
	}


	@Transactional
	public Booking cancelBooking(UUID id) {
		Booking existing = getBooking(id);
		existing.setStatus(BookingStatus.CANCELED);
		Booking saved = bookingRepository.save(existing);
		
		// Sync to Allocation table
		var allocations = allocationRepository.findAll();
		allocations.stream()
			.filter(a -> a.getEntityId().equals(id) && a.getType() == AllocationType.BOOKING)
			.forEach(a -> {
				a.setStatus(AllocationStatus.CANCELED);
				allocationRepository.save(a);
			});
		
		return saved;
	}

	@Transactional
	public Booking rebookBooking(UUID id) {
		Booking existing = getBooking(id);
		if (existing.getStatus() == BookingStatus.ACTIVE) {
			return existing;
		}
		allocationPolicy.ensureDatesAvailableFor(existing, existing.getId());
		existing.setStatus(BookingStatus.ACTIVE);
		return bookingRepository.save(existing);
	}

	@Transactional
	public void deleteBooking(UUID id) {
		if (!bookingRepository.existsById(id)) {
			return;
		}
		bookingRepository.deleteById(id);
		
		// Sync to Allocation table (1 query instead of N+1)
		allocationRepository.deleteByEntityId(id);
	}

	@Transactional(readOnly = true)
	public List<Booking> listByPropertyAndMonth(String propertyId, int year, int month) {
		YearMonth ym = YearMonth.of(year, month);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();
		return bookingRepository.findOverlappingBookings(propertyId, start, end);
	}

	private void validateInputs(String propertyId,
	                            String guestName,
	                            String guestEmail,
	                            LocalDate startDate,
	                            LocalDate endDate) {
		if (!StringUtils.hasText(propertyId)) {
			throw new IllegalArgumentException(getMessage("error.validation.propertyId.required"));
		}
		if (!StringUtils.hasText(guestName)) {
			throw new IllegalArgumentException(getMessage("error.validation.guestName.required"));
		}
		if (!StringUtils.hasText(guestEmail)) {
			throw new IllegalArgumentException(getMessage("error.validation.guestEmail.required"));
		}
		if (startDate == null || endDate == null) {
			throw new IllegalArgumentException(getMessage("error.validation.dates.required"));
		}
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException(getMessage("error.validation.dateRange.invalid"));
		}
	}

	private String getMessage(String code) {
		return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
	}
}


