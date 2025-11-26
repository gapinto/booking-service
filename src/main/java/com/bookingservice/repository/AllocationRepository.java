package com.bookingservice.repository;

import com.bookingservice.model.Allocation;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.model.AllocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

	@Query("""
		select a from Allocation a
		where a.propertyId = :propertyId
		  and a.status = :status
		  and a.startDate <= :endDate
		  and a.endDate >= :startDate
	""")
	List<Allocation> findOverlappingAllocations(
		@Param("propertyId") String propertyId,
		@Param("status") AllocationStatus status,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	Optional<Allocation> findByEntityId(UUID entityId);

	@Modifying
	@Query("""
		update Allocation a
		set a.propertyId = :propertyId,
		    a.startDate = :startDate,
		    a.endDate = :endDate
		where a.entityId = :entityId
	""")
	void updateByEntityId(
		@Param("entityId") UUID entityId,
		@Param("propertyId") String propertyId,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	@Modifying
	@Query("delete from Allocation a where a.entityId = :entityId")
	void deleteByEntityId(@Param("entityId") UUID entityId);

	@Modifying
	@Query("update Allocation a set a.status = :status where a.entityId = :entityId and a.type = :type")
	void updateStatusByEntityIdAndType(
		@Param("entityId") UUID entityId,
		@Param("type") AllocationType type,
		@Param("status") AllocationStatus status
	);
}


