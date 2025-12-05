package com.thiefspin.bookingsystem.controllers;

import com.thiefspin.bookingsystem.branches.Branch;
import com.thiefspin.bookingsystem.branches.BranchService;
import com.thiefspin.bookingsystem.util.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    @Mock
    private BranchService branchService;

    @InjectMocks
    private BranchController branchController;

    private Branch testBranch;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        testBranch = new Branch(
            1L,
            "BR001",
            "Main Branch",
            "123 Main St, City",
            "+1-555-0100",
            LocalTime.of(9, 0),
            LocalTime.of(17, 0),
            5
        );
    }

    @Nested
    @DisplayName("List Branches")
    class ListBranchesTests {

        @Test
        @DisplayName("should return paginated list of branches")
        void shouldReturnPaginatedBranches() {
            // Given
            List<Branch> branches = Arrays.asList(testBranch);
            Page<Branch> page = new PageImpl<>(branches, pageable, branches.size());
            when(branchService.list(any(Pageable.class))).thenReturn(page);

            // When
            Page<Branch> result = branchController.list(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testBranch);
            verify(branchService).list(pageable);
        }

        @Test
        @DisplayName("should return empty page when no branches exist")
        void shouldReturnEmptyPage() {
            // Given
            Page<Branch> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(branchService.list(any(Pageable.class))).thenReturn(emptyPage);

            // When
            Page<Branch> result = branchController.list(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(branchService).list(pageable);
        }
    }

    @Nested
    @DisplayName("Find Branch By ID")
    class FindByIdTests {

        @Test
        @DisplayName("should return branch when found")
        void shouldReturnBranchWhenFound() throws NotFoundException {
            // Given
            Long branchId = 1L;
            when(branchService.findById(branchId)).thenReturn(Optional.of(testBranch));

            // When
            Branch result = branchController.findById(branchId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testBranch);
            verify(branchService).findById(branchId);
        }

        @Test
        @DisplayName("should throw NotFoundException when branch not found")
        void shouldThrowNotFoundExceptionWhenBranchNotFound() {
            // Given
            Long branchId = 999L;
            when(branchService.findById(branchId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> branchController.findById(branchId))
                .isInstanceOf(NotFoundException.class);
            verify(branchService).findById(branchId);
        }
    }

    @Nested
    @DisplayName("Search Branches")
    class SearchBranchesTests {

        @Test
        @DisplayName("should return matching branches when searching by name")
        void shouldReturnMatchingBranchesByName() {
            // Given
            String searchQuery = "Main";
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).contains("Main");
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should return matching branches when searching by address")
        void shouldReturnMatchingBranchesByAddress() {
            // Given
            String searchQuery = "123 Main St";
            Branch branch2 = new Branch(
                2L,
                "BR002",
                "Downtown Branch",
                "456 Downtown Ave",
                "+1-555-0200",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                3
            );
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).address()).contains("123 Main St");
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should return matching branches when searching by code")
        void shouldReturnMatchingBranchesByCode() {
            // Given
            String searchQuery = "BR001";
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).code()).isEqualTo("BR001");
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should return empty page when no matches found")
        void shouldReturnEmptyPageWhenNoMatches() {
            // Given
            String searchQuery = "NonExistent";
            Page<Branch> emptyResults = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(emptyResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should handle partial matches")
        void shouldHandlePartialMatches() {
            // Given
            String searchQuery = "Mai";
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should handle case-insensitive search")
        void shouldHandleCaseInsensitiveSearch() {
            // Given
            String searchQuery = "main";
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(anyString(), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Main Branch");
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should trim search query")
        void shouldTrimSearchQuery() {
            // Given
            String searchQuery = "  Main  ";
            List<Branch> matchingBranches = Arrays.asList(testBranch);
            Page<Branch> searchResults = new PageImpl<>(matchingBranches, pageable, matchingBranches.size());
            when(branchService.search(eq(searchQuery), any(Pageable.class))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(branchService).search(searchQuery, pageable);
        }

        @Test
        @DisplayName("should support pagination in search results")
        void shouldSupportPaginationInSearch() {
            // Given
            String searchQuery = "Branch";
            Pageable customPageable = PageRequest.of(1, 5);
            Branch branch2 = new Branch(
                2L,
                "BR002",
                "Second Branch",
                "456 Second St",
                "+1-555-0200",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                3
            );
            List<Branch> matchingBranches = Arrays.asList(testBranch, branch2);
            Page<Branch> searchResults = new PageImpl<>(
                matchingBranches,
                customPageable,
                10 // Total elements
            );
            when(branchService.search(anyString(), eq(customPageable))).thenReturn(searchResults);

            // When
            Page<Branch> result = branchController.search(searchQuery, customPageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getNumber()).isEqualTo(1); // Page number
            assertThat(result.getSize()).isEqualTo(5); // Page size
            assertThat(result.getTotalElements()).isEqualTo(10);
            verify(branchService).search(searchQuery, customPageable);
        }
    }
}
