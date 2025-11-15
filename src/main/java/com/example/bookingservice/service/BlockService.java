package com.example.bookingservice.service;

import com.example.bookingservice.api.dto.BlockDtos;
import com.example.bookingservice.api.mapper.BlockMapper;
import com.example.bookingservice.model.Block;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.repository.BlockRepository;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.service.policy.AvailabilityPolicy;
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
	private final BookingRepository bookingRepository;
	private final AvailabilityPolicy availabilityPolicy;
	private final BlockMapper blockMapper;
	private final MessageSource messageSource;

	public BlockService(BlockRepository blockRepository, BookingRepository bookingRepository, AvailabilityPolicy availabilityPolicy, BlockMapper blockMapper, MessageSource messageSource) {
		this.blockRepository = blockRepository;
		this.bookingRepository = bookingRepository;
		this.availabilityPolicy = availabilityPolicy;
		this.blockMapper = blockMapper;
		this.messageSource = messageSource;
	}

	@Transactional
	public Block createBlock(Block block) {
		if (block == null) {
			throw new IllegalArgumentException(getMessage("error.validation.block.required"));
		}
		validate(block.getPropertyId(), block.getStartDate(), block.getEndDate());
		availabilityPolicy.ensureBlockDatesAllowed(block.getPropertyId(), block.getStartDate(), block.getEndDate(), null);
		return blockRepository.save(block);
	}

	@Transactional
	public Block updateBlock(UUID blockId, BlockDtos.CreateOrUpdateRequest update) {
		Block existing = blockRepository.findById(blockId)
			.orElseThrow(() -> new IllegalArgumentException("Block not found: " + blockId));
		// validate and check availability using incoming values BEFORE mutating entity
		validate(update.propertyId(), update.startDate(), update.endDate());
		availabilityPolicy.ensureBlockDatesAllowed(update.propertyId(), update.startDate(), update.endDate(), existing.getId());
		// apply incoming values explicitly and persist
		existing.setPropertyId(update.propertyId());
		existing.setStartDate(update.startDate());
		existing.setEndDate(update.endDate());
		return blockRepository.save(existing);
	}

	@Transactional
	public void deleteBlock(UUID blockId) {
		if (!blockRepository.existsById(blockId)) {
			return;
		}
		blockRepository.deleteById(blockId);
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


