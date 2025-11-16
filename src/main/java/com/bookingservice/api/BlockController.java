package com.bookingservice.api;

import com.bookingservice.api.dto.BlockDtos;
import com.bookingservice.model.Block;
import com.bookingservice.service.BlockService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
@Tag(name = "Blocks", description = "Endpoints to manage property blocks")
public class BlockController {

	private final BlockService blockService;

	public BlockController(BlockService blockService) {
		this.blockService = blockService;
	}

	@PostMapping
	@Operation(summary = "Create block", description = "Create a block for a date range, preventing new bookings")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = BlockDtos.Response.class))),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "409", description = "Conflict with active booking or another block")
	})
	public ResponseEntity<BlockDtos.Response> create(@Valid @RequestBody BlockDtos.CreateOrUpdateRequest req) {
		Block created = blockService.createBlock(req.toEntity());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.getId())
			.toUri();
		return ResponseEntity.created(location).body(BlockDtos.Response.from(created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update block", description = "Update an existing block")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BlockDtos.Response.class))),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "404", description = "Not found"),
		@ApiResponse(responseCode = "409", description = "Conflict with active booking or another block")
	})
	public BlockDtos.Response update(@PathVariable("id") UUID id, @Valid @RequestBody BlockDtos.CreateOrUpdateRequest req) {
		return BlockDtos.Response.from(blockService.updateBlock(id, req));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete block", description = "Remove a block from the system (idempotent)")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "No Content"),
		@ApiResponse(responseCode = "404", description = "Not found")
	})
	public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
		blockService.deleteBlock(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/calendar")
	@Operation(summary = "List blocks by property and month", description = "Returns blocks overlapping the given month")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK")
	})
	public ResponseEntity<java.util.List<BlockDtos.Response>> listByMonth(
		@RequestParam String propertyId,
		@RequestParam int year,
		@RequestParam int month
	) {
		var list = blockService.listByPropertyAndMonth(propertyId, year, month)
			.stream().map(BlockDtos.Response::from).toList();
		return ResponseEntity.ok(list);
	}
}


