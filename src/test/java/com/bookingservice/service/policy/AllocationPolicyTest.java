package com.bookingservice.service.policy;

import com.bookingservice.model.Allocation;
import com.bookingservice.model.Allocatable;
import com.bookingservice.model.AllocationType;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.repository.AllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AllocationPolicy - Given/When/Then")
class AllocationPolicyTest {

	@Autowired
	private AllocationRepository allocationRepository;
	@Autowired
	private AllocationPolicy allocationPolicy;

	private static final String PROPERTY = "property-123";

	@BeforeEach
	void resetDb() {
		allocationRepository.deleteAll();
	}

	private Allocatable createAllocatable(String propertyId, LocalDate start, LocalDate end, AllocationType type) {
		return new Allocatable() {
			@Override
			public UUID getId() {
				return UUID.randomUUID();
			}

			@Override
			public String getPropertyId() {
				return propertyId;
			}

			@Override
			public LocalDate getStartDate() {
				return start;
			}

			@Override
			public LocalDate getEndDate() {
				return end;
			}

			@Override
			public AllocationType getAllocationType() {
				return type;
			}
		};
	}

	@Test
	@DisplayName("Given no allocations When checking dates Then no exception")
	void givenNoAllocations_whenCheckDates_thenNoException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(1);
		LocalDate end = start.plusDays(3);
		Allocatable allocatable = createAllocatable(PROPERTY, start, end, AllocationType.BOOKING);

		// When / Then
		assertDoesNotThrow(() -> 
			allocationPolicy.ensureDatesAvailableFor(allocatable, null)
		);
	}

	@Test
	@DisplayName("Given overlapping active allocation When checking Then exception")
	void givenOverlappingAllocation_whenCheck_thenException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(5);
		LocalDate end = start.plusDays(2);
		Allocation existing = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(existing);

		LocalDate newStart = start.plusDays(1);
		LocalDate newEnd = end.plusDays(1);
		Allocatable allocatable = createAllocatable(PROPERTY, newStart, newEnd, AllocationType.BOOKING);

		// When / Then
		assertThrows(IllegalStateException.class, () ->
			allocationPolicy.ensureDatesAvailableFor(allocatable, null)
		);
	}

	@Test
	@DisplayName("Given canceled allocation When checking Then no exception")
	void givenCanceledAllocation_whenCheck_thenNoException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(10);
		LocalDate end = start.plusDays(2);
		Allocation canceled = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BOOKING)
			.status(AllocationStatus.CANCELED)  // Canceled
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(canceled);

		Allocatable allocatable = createAllocatable(PROPERTY, start, end, AllocationType.BOOKING);

		// When / Then
		assertDoesNotThrow(() ->
			allocationPolicy.ensureDatesAvailableFor(allocatable, null)
		);
	}

	@Test
	@DisplayName("Given same entity excluded When checking Then no exception")
	void givenSameEntityExcluded_whenCheck_thenNoException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(15);
		LocalDate end = start.plusDays(2);
		UUID entityId = UUID.randomUUID();
		Allocation existing = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BOOKING)
			.status(AllocationStatus.ACTIVE)
			.entityId(entityId)
			.build();
		allocationRepository.save(existing);

		// Create allocatable with same entityId to be excluded
		Allocatable allocatable = new Allocatable() {
			@Override
			public UUID getId() {
				return entityId;
			}

			@Override
			public String getPropertyId() {
				return PROPERTY;
			}

			@Override
			public LocalDate getStartDate() {
				return start;
			}

			@Override
			public LocalDate getEndDate() {
				return end;
			}

			@Override
			public AllocationType getAllocationType() {
				return AllocationType.BOOKING;
			}
		};

		// When / Then (same entity excluded)
		assertDoesNotThrow(() ->
			allocationPolicy.ensureDatesAvailableFor(allocatable, entityId)
		);
	}

	@Test
	@DisplayName("Given no overlap with different property When checking Then no exception")
	void givenDifferentProperty_whenCheck_thenNoException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(20);
		LocalDate end = start.plusDays(2);
		Allocation existing = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId("other-property")  // Different
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(existing);

		Allocatable allocatable = createAllocatable(PROPERTY, start, end, AllocationType.BOOKING);

		// When / Then
		assertDoesNotThrow(() ->
			allocationPolicy.ensureDatesAvailableFor(allocatable, null)
		);
	}

	@Test
	@DisplayName("Given overlapping only end date When checking Then exception")
	void givenOverlappingEndDate_whenCheck_thenException() {
		// Given
		LocalDate start = LocalDate.now().plusDays(30);
		LocalDate end = start.plusDays(2);
		Allocation existing = Allocation.builder()
			.id(UUID.randomUUID())
			.propertyId(PROPERTY)
			.startDate(start)
			.endDate(end)
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(UUID.randomUUID())
			.build();
		allocationRepository.save(existing);

		// New range starts before existing ends
		LocalDate newStart = end.minusDays(1);
		LocalDate newEnd = end.plusDays(1);
		Allocatable allocatable = createAllocatable(PROPERTY, newStart, newEnd, AllocationType.BOOKING);

		// When / Then
		assertThrows(IllegalStateException.class, () ->
			allocationPolicy.ensureDatesAvailableFor(allocatable, null)
		);
	}
}


