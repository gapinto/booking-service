package com.bookingservice.service;

import com.bookingservice.api.dto.BlockDtos;
import com.bookingservice.model.Allocation;
import com.bookingservice.model.AllocationType;
import com.bookingservice.model.AllocationStatus;
import com.bookingservice.model.Block;
import com.bookingservice.repository.AllocationRepository;
import com.bookingservice.repository.BlockRepository;
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
public class BlockService {
	private final BlockRepository blockRepository;
	private final AllocationRepository allocationRepository;
	private final AllocationPolicy allocationPolicy;
	private final MessageSource messageSource;

	public BlockService(BlockRepository blockRepository, AllocationRepository allocationRepository, AllocationPolicy allocationPolicy, MessageSource messageSource) {
		this.blockRepository = blockRepository;
		this.allocationRepository = allocationRepository;
		this.allocationPolicy = allocationPolicy;
		this.messageSource = messageSource;
	}

	@Transactional
	public Block createBlock(Block block) {
		if (block == null) {
			throw new IllegalArgumentException(getMessage("error.validation.block.required"));
		}
		validate(block.getPropertyId(), block.getStartDate(), block.getEndDate());
		allocationPolicy.ensureDatesAvailableFor(block, null);
		Block saved = blockRepository.save(block);
		
		// Sync to Allocation table
		Allocation allocation = Allocation.builder()
			.id(java.util.UUID.randomUUID())
			.propertyId(saved.getPropertyId())
			.startDate(saved.getStartDate())
			.endDate(saved.getEndDate())
			.type(AllocationType.BLOCK)
			.status(AllocationStatus.ACTIVE)
			.entityId(saved.getId())
			.build();
		allocationRepository.save(allocation);
		
		return saved;
	}

	@Transactional
	public Block updateBlock(UUID blockId, BlockDtos.CreateOrUpdateRequest update) {
		Block existing = blockRepository.findById(blockId)
			.orElseThrow(() -> new IllegalArgumentException("Block not found: " + blockId));
		// validate and check availability using incoming values BEFORE mutating entity
		validate(update.propertyId(), update.startDate(), update.endDate());
		// validate availability with the requested block (without persisting)
		Block requestedBlock = update.toEntity();
		allocationPolicy.ensureDatesAvailableFor(requestedBlock, existing.getId());
		// apply incoming values explicitly and persist
		existing.setPropertyId(update.propertyId());
		existing.setStartDate(update.startDate());
		existing.setEndDate(update.endDate());
		Block saved = blockRepository.save(existing);
		
		// Sync to Allocation table (1 query instead of N+1)
		allocationRepository.updateByEntityId(
			blockId,
			saved.getPropertyId(),
			saved.getStartDate(),
			saved.getEndDate()
		);

		return saved;
	}

	@Transactional
	public void deleteBlock(UUID blockId) {
		if (!blockRepository.existsById(blockId)) {
			return;
		}
		blockRepository.deleteById(blockId);
		
		// Sync to Allocation table (1 query instead of N+1)
		allocationRepository.deleteByEntityId(blockId);
	}

	private void validate(String propertyId, LocalDate startDate, LocalDate endDate) {
		if (!StringUtils.hasText(propertyId)) {
			throw new IllegalArgumentException(getMessage("error.validation.propertyId.required"));
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

	@Transactional(readOnly = true)
	public List<Block> listByPropertyAndMonth(String propertyId, int year, int month) {
		YearMonth ym = YearMonth.of(year, month);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();
		return blockRepository.findOverlappingBlocks(propertyId, start, end);
	}
}


