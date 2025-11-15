package com.example.bookingservice.repository;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@Query("""
		select b from Booking b
		where b.propertyId = :propertyId
		  and b.status <> :excludedStatus
		  and b.startDate <= :endDate
		  and b.endDate >= :startDate
	""")
	List<Booking> findOverlappingActiveBookings(
		@Param("propertyId") String propertyId,
		@Param("excludedStatus") BookingStatus excludedStatus,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	@Query("""
		select b from Booking b
		where b.propertyId = :propertyId
		  and b.startDate <= :endDate
		  and b.endDate >= :startDate
	""")
	List<Booking> findOverlappingBookings(
		@Param("propertyId") String propertyId,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

}


