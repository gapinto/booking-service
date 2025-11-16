package com.bookingservice.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Interface for entities that can be allocated (reserved/blocked).
 * Defines the contract for any entity that occupies date ranges on a property.
 */
public interface Allocatable {
	UUID getId();
	
	String getPropertyId();
	
	LocalDate getStartDate();
	
	LocalDate getEndDate();
	
	/**
	 * Determines the type of allocation this entity represents.
	 * @return AllocationType.BOOKING for bookings, AllocationType.BLOCK for blocks
	 */
	AllocationType getAllocationType();
}


