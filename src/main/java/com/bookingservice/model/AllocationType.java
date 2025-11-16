package com.bookingservice.model;

/**
 * Types of allocations with their corresponding error message keys.
 */
public enum AllocationType {
	BOOKING("error.allocation.conflict.booking"),
	BLOCK("error.allocation.conflict.block");

	private final String conflictMessageKey;

	AllocationType(String conflictMessageKey) {
		this.conflictMessageKey = conflictMessageKey;
	}

	public String getConflictMessageKey() {
		return conflictMessageKey;
	}
}


