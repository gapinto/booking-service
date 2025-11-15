package com.example.bookingservice.repository;

import com.example.bookingservice.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {

	@Query("""
		select bl from Block bl
		where bl.propertyId = :propertyId
		  and bl.startDate <= :endDate
		  and bl.endDate >= :startDate
	""")
	List<Block> findOverlappingBlocks(
		@Param("propertyId") String propertyId,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);
}


