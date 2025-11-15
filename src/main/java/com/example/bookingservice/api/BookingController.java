package com.example.bookingservice.api;

import com.example.bookingservice.api.dto.BookingDtos;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.service.BookingService;
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
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Endpoints to manage bookings")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@Operation(summary = "Create booking", description = "Create a booking if dates are available")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = BookingDtos.Response.class))),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "409", description = "Conflict with existing booking/block")
	})
	public ResponseEntity<BookingDtos.Response> create(@Valid @RequestBody BookingDtos.CreateRequest req) {
		Booking b = new Booking();
		b.setPropertyId(req.propertyId());
		b.setGuestName(req.guestName());
		b.setGuestEmail(req.guestEmail());
		b.setStartDate(req.startDate());
		b.setEndDate(req.endDate());
		Booking created = bookingService.createBooking(b);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.getId())
			.toUri();
		return ResponseEntity.created(location).body(BookingDtos.Response.from(created));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get booking", description = "Get a booking by id")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BookingDtos.Response.class))),
		@ApiResponse(responseCode = "404", description = "Not found")
	})
	public BookingDtos.Response get(@PathVariable("id") UUID id) {
		return BookingDtos.Response.from(bookingService.getBooking(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update booking", description = "Update guest details and date range")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BookingDtos.Response.class))),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "404", description = "Not found"),
		@ApiResponse(responseCode = "409", description = "Conflict with existing booking/block")
	})
	public BookingDtos.Response update(@PathVariable("id") UUID id, @Valid @RequestBody BookingDtos.UpdateRequest req) {
		return BookingDtos.Response.from(bookingService.updateBooking(id, req));
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel booking", description = "Cancel a booking (status = CANCELED)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BookingDtos.Response.class))),
		@ApiResponse(responseCode = "404", description = "Not found")
	})
	public BookingDtos.Response cancel(@PathVariable("id") UUID id) {
		return BookingDtos.Response.from(bookingService.cancelBooking(id));
	}

	@PostMapping("/{id}/rebook")
	@Operation(summary = "Rebook canceled booking", description = "Reactivate a canceled booking if dates are available")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BookingDtos.Response.class))),
		@ApiResponse(responseCode = "404", description = "Not found"),
		@ApiResponse(responseCode = "409", description = "Conflict with existing booking/block")
	})
	public BookingDtos.Response rebook(@PathVariable("id") UUID id) {
		return BookingDtos.Response.from(bookingService.rebookBooking(id));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete booking", description = "Remove booking from the system (idempotent)")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "No Content"),
		@ApiResponse(responseCode = "404", description = "Not found")
	})
	public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
		bookingService.deleteBooking(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/calendar")
	@Operation(summary = "List bookings by property and month", description = "Returns bookings overlapping the given month")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK")
	})
	public ResponseEntity<java.util.List<BookingDtos.Response>> listByMonth(
		@RequestParam String propertyId,
		@RequestParam int year,
		@RequestParam int month
	) {
		var list = bookingService.listByPropertyAndMonth(propertyId, year, month)
			.stream().map(BookingDtos.Response::from).toList();
		return ResponseEntity.ok(list);
	}

}


