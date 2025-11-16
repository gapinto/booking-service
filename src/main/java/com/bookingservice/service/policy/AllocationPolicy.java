package com.bookingservice.service.policy;

import com.bookingservice.model.Allocation;
import com.bookingservice.model.Allocatable;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.repository.AllocationRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized allocation policy for checking date availability.
 * Uses a single Allocation table for both Bookings and Blocks.
 * Scales efficiently to millions of allocations with proper indexing.
 */
@Component
public class AllocationPolicy {

	private final AllocationRepository allocationRepository;
	private final MessageSource messageSource;

	public AllocationPolicy(AllocationRepository allocationRepository, MessageSource messageSource) {
		this.allocationRepository = allocationRepository;
		this.messageSource = messageSource;
	}

	/**
	 * Checks if dates are available for an Allocatable entity.
	 * Only checks against ACTIVE allocations.
	 * For creates: pass null as excludedEntityId.
	 * For updates: pass the current entity ID as excludedEntityId to allow self-updates.
	 *
	 * @param allocatable the entity that wants to allocate dates (Booking or Block)
	 * @param excludedEntityId ID to exclude (for updates), null for creates
	 * @throws IllegalStateException if overlap is found
	 */
	public void ensureDatesAvailableFor(Allocatable allocatable, UUID excludedEntityId) {
		var overlappingAllocations = allocationRepository
			.findOverlappingAllocations(
				allocatable.getPropertyId(),
				AllocationStatus.ACTIVE,
				allocatable.getStartDate(),
				allocatable.getEndDate()
			);

		boolean hasConflict = overlappingAllocations.stream()
			.anyMatch(allocation -> isConflicting(allocation, excludedEntityId));

		if (hasConflict) {
			String msg = messageSource.getMessage(
				allocatable.getAllocationType().getConflictMessageKey(),
				new Object[]{allocatable.getStartDate(), allocatable.getEndDate(), allocatable.getPropertyId()},
				LocaleContextHolder.getLocale()
			);
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * Checks if an allocation conflicts with the excluded entity ID.
	 * For creates: excludedEntityId is null, so any allocation is a conflict.
	 * For updates: excludedEntityId is the current entity, allowing self-updates.
	 *
	 * @param allocation the allocation to check
	 * @param excludedEntityId the entity ID to exclude from conflict check (null for creates)
	 * @return true if the allocation is a conflict, false if it should be ignored
	 */
	private boolean isConflicting(Allocation allocation, UUID excludedEntityId) {
		// For creates (excludedEntityId == null), any overlap is a conflict
		if (excludedEntityId == null) {
			return true;
		}
		// For updates, ignore overlaps from the same entity being updated
		return !allocation.getEntityId().equals(excludedEntityId);
	}
}


