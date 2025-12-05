package com.thiefspin.bookingsystem.controllers;

import com.thiefspin.bookingsystem.branches.Branch;
import com.thiefspin.bookingsystem.branches.BranchService;
import com.thiefspin.bookingsystem.util.exceptions.ApiErrorResponse;
import com.thiefspin.bookingsystem.util.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/branches")
public class BranchController {

  private BranchService service;

  @GetMapping
  @Operation(
      summary = "List branches",
      description = "Returns a paginated list of branches. Supports page, size and sort query parameters."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Paginated list of branches",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Branch.class)
          )
      )
  })
  public Page<Branch> list(@ParameterObject Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get a branch by ID",
      description = "Returns details for a single branch."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Branch found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Branch.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Branch not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      )
  })
  public Branch findById(@PathVariable Long id) throws NotFoundException {
    return service.findById(id).orElseThrow(() -> new NotFoundException("Branch not found"));
  }

  @GetMapping("/search")
  @Operation(
      summary = "Search branches",
      description = "Search branches by name, address, or code. Returns a paginated list of matching branches."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Paginated list of matching branches",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Branch.class)
          )
      )
  })
  public Page<Branch> search(
      @RequestParam(required = true) String query,
      @ParameterObject Pageable pageable
  ) {
    return service.search(query, pageable);
  }

}
