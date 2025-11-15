package com.example.bookingservice.api.dto;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class BookingDtos {

	public record CreateRequest(
		@NotBlank String propertyId,
		@NotBlank String guestName,
		@NotBlank @Email String guestEmail,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate
	) {}

	public record UpdateRequest(
		@NotBlank String guestName,
		@NotBlank @Email String guestEmail,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate
	) {}

	public record Response(
		UUID id,
		String propertyId,
		String guestName,
		String guestEmail,
		LocalDate startDate,
		LocalDate endDate,
		BookingStatus status
	) {
		public static Response from(Booking b) {
			return new Response(
				b.getId(),
				b.getPropertyId(),
				b.getGuestName(),
				b.getGuestEmail(),
				b.getStartDate(),
				b.getEndDate(),
				b.getStatus()
			);
		}
	}
}


