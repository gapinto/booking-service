package com.example.bookingservice.api.dto;

import com.example.bookingservice.model.Block;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class BlockDtos {
	public record CreateOrUpdateRequest(
		@NotBlank String propertyId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate
	) {}

	public record Response(
		UUID id,
		String propertyId,
		LocalDate startDate,
		LocalDate endDate
	) {
		public static Response from(Block b) {
			return new Response(b.getId(), b.getPropertyId(), b.getStartDate(), b.getEndDate());
		}
	}
}


