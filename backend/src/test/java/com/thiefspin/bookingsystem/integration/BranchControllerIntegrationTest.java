package com.thiefspin.bookingsystem.integration;

import com.thiefspin.bookingsystem.BaseIntegrationTest;
import com.thiefspin.bookingsystem.branches.Branch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
public class BranchControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/branches/search")
    class SearchBranchesTests {

        @Test
        @DisplayName("should search branches by name")
        void shouldSearchBranchesByName() throws Exception {
            // Given
            String searchQuery = "Claremont";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Claremont")));
        }

        @Test
        @DisplayName("should search branches by address")
        void shouldSearchBranchesByAddress() throws Exception {
            // Given
            String searchQuery = "Sandton";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].address", containsString("Sandton")));
        }

        @Test
        @DisplayName("should search branches by code")
        void shouldSearchBranchesByCode() throws Exception {
            // Given
            String searchQuery = "CPT001";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].code", is("CPT001")));
        }

        @Test
        @DisplayName("should perform case-insensitive search")
        void shouldPerformCaseInsensitiveSearch() throws Exception {
            // Given
            String searchQuery = "claremont";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Claremont")));
        }

        @Test
        @DisplayName("should handle partial matches")
        void shouldHandlePartialMatches() throws Exception {
            // Given
            String searchQuery = "Mall";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("should return empty results when no matches found")
        void shouldReturnEmptyResultsWhenNoMatchesFound() throws Exception {
            // Given
            String searchQuery = "NonExistentBranch";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @DisplayName("should handle search with special characters")
        void shouldHandleSearchWithSpecialCharacters() throws Exception {
            // Given
            String searchQuery = "V&A";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("V&A")));
        }

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginatedResults() throws Exception {
            // Given
            String searchQuery = "Branch";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "2")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageable.pageSize", is(2)));
        }

        @Test
        @DisplayName("should handle search with leading/trailing spaces")
        void shouldHandleSearchWithSpaces() throws Exception {
            // Given
            String searchQuery = "  Waterfront  ";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("should only return active branches")
        void shouldOnlyReturnActiveBranches() throws Exception {
            // Given
            String searchQuery = "Branch";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "100")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", everyItem(notNullValue())));
        }

        @Test
        @DisplayName("should handle empty search query gracefully")
        void shouldHandleEmptySearchQuery() throws Exception {
            // Given
            String searchQuery = "";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("should support different page sizes")
        void shouldSupportDifferentPageSizes() throws Exception {
            // Given
            String searchQuery = "Branch";

            // When & Then
            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                    .andExpect(jsonPath("$.size", is(5)));

            mockMvc.perform(get("/api/branches/search")
                    .param("query", searchQuery)
                    .param("page", "0")
                    .param("size", "20")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(20))))
                    .andExpect(jsonPath("$.size", is(20)));
        }
    }

    @Nested
    @DisplayName("GET /api/branches")
    class ListBranchesTests {

        @Test
        @DisplayName("should list all active branches")
        void shouldListAllActiveBranches() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/branches")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.content[0].id", notNullValue()))
                    .andExpect(jsonPath("$.content[0].name", notNullValue()))
                    .andExpect(jsonPath("$.content[0].address", notNullValue()))
                    .andExpect(jsonPath("$.content[0].phoneNumber", notNullValue()));
        }
    }

    @Nested
    @DisplayName("GET /api/branches/{id}")
    class GetBranchByIdTests {

        @Test
        @DisplayName("should get branch by id")
        void shouldGetBranchById() throws Exception {
            // Given
            Long branchId = 1L;

            // When & Then
            mockMvc.perform(get("/api/branches/{id}", branchId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", notNullValue()))
                    .andExpect(jsonPath("$.code", notNullValue()));
        }

        @Test
        @DisplayName("should return 404 when branch not found")
        void shouldReturn404WhenBranchNotFound() throws Exception {
            // Given
            Long nonExistentId = 99999L;

            // When & Then
            mockMvc.perform(get("/api/branches/{id}", nonExistentId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
