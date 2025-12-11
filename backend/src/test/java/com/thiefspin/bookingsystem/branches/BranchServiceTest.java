package com.thiefspin.bookingsystem.branches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("BranchService Tests")
class BranchServiceTest {

  @Mock
  private BranchRepository repository;

  @InjectMocks
  private BranchService service;

  private BranchEntity testBranchEntity;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);

    testBranchEntity = new BranchEntity(
        1L,
        "JHB-001",
        "Johannesburg Central",
        "123 Main Street",
        "+27111234567",
        "jhb@example.com",
        LocalTime.of(9, 0),
        LocalTime.of(17, 0),
        3,
        true,
        Instant.now(),
        Instant.now()
    );
  }

  @Nested
  @DisplayName("List Branches Tests")
  class ListBranchesTests {

    @Test
    @DisplayName("Should return paginated list of branches")
    void shouldReturnPaginatedList() {
      // Given
      Page<BranchEntity> entityPage = new PageImpl<>(List.of(testBranchEntity), pageable, 1);
      when(repository.findAll(pageable)).thenReturn(entityPage);

      // When
      Page<Branch> result = service.list(pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).code()).isEqualTo("JHB-001");
      assertThat(result.getTotalElements()).isEqualTo(1);
      verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no branches exist")
    void shouldReturnEmptyPage() {
      // Given
      Page<BranchEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
      when(repository.findAll(pageable)).thenReturn(emptyPage);

      // When
      Page<Branch> result = service.list(pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Should respect pagination parameters")
    void shouldRespectPaginationParameters() {
      // Given
      Pageable customPageable = PageRequest.of(2, 20);
      BranchEntity branch2 = new BranchEntity(
          2L, "CPT-001", "Cape Town Branch", "456 Long Street",
          "+27211234567", "cpt@example.com", LocalTime.of(8, 0), LocalTime.of(18, 0), 5,
          true, Instant.now(), Instant.now()
      );
      Page<BranchEntity> entityPage = new PageImpl<>(
          List.of(testBranchEntity, branch2),
          customPageable,
          100
      );
      when(repository.findAll(customPageable)).thenReturn(entityPage);

      // When
      Page<Branch> result = service.list(customPageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getNumber()).isEqualTo(2);
      assertThat(result.getSize()).isEqualTo(20);
      assertThat(result.getTotalElements()).isEqualTo(100);
      verify(repository).findAll(customPageable);
    }

    @Test
    @DisplayName("Should convert all branch entities to models")
    void shouldConvertAllEntitiesToModels() {
      // Given
      BranchEntity branch2 = new BranchEntity(
          2L, "CPT-001", "Cape Town", "456 Long St",
          "+27211234567", "cpt@example.com", LocalTime.of(8, 0), LocalTime.of(18, 0), 5,
          true, Instant.now(), Instant.now()
      );
      Page<BranchEntity> entityPage = new PageImpl<>(List.of(testBranchEntity, branch2), pageable,
          2);
      when(repository.findAll(pageable)).thenReturn(entityPage);

      // When
      Page<Branch> result = service.list(pageable);

      // Then
      assertThat(result.getContent()).allSatisfy(branch -> {
        assertThat(branch).isNotNull();
        assertThat(branch.id()).isNotNull();
        assertThat(branch.code()).isNotNull();
        assertThat(branch.name()).isNotNull();
      });
      verify(repository).findAll(pageable);
    }
  }

  @Nested
  @DisplayName("Find By ID Tests")
  class FindByIdTests {

    @Test
    @DisplayName("Should return branch when found")
    void shouldReturnBranchWhenFound() {
      // Given
      when(repository.findById(1L)).thenReturn(Optional.of(testBranchEntity));

      // When
      Optional<Branch> result = service.findById(1L);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(1L);
      assertThat(result.get().code()).isEqualTo("JHB-001");
      assertThat(result.get().name()).isEqualTo("Johannesburg Central");
      verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when branch not found")
    void shouldReturnEmptyWhenNotFound() {
      // Given
      when(repository.findById(999L)).thenReturn(Optional.empty());

      // When
      Optional<Branch> result = service.findById(999L);

      // Then
      assertThat(result).isEmpty();
      verify(repository).findById(999L);
    }

    @Test
    @DisplayName("Should handle multiple calls to same ID")
    void shouldHandleMultipleCalls() {
      // Given
      when(repository.findById(1L)).thenReturn(Optional.of(testBranchEntity));

      // When
      Optional<Branch> result1 = service.findById(1L);
      Optional<Branch> result2 = service.findById(1L);

      // Then
      assertThat(result1).isPresent();
      assertThat(result2).isPresent();
      assertThat(result1.get().id()).isEqualTo(result2.get().id());
      assertThat(result1.get().code()).isEqualTo(result2.get().code());
      verify(repository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("Should convert entity to model correctly")
    void shouldConvertEntityToModel() {
      // Given
      when(repository.findById(1L)).thenReturn(Optional.of(testBranchEntity));

      // When
      Optional<Branch> result = service.findById(1L);

      // Then
      assertThat(result).isPresent();
      Branch branch = result.get();
      assertThat(branch.id()).isEqualTo(testBranchEntity.id());
      assertThat(branch.code()).isEqualTo(testBranchEntity.code());
      assertThat(branch.name()).isEqualTo(testBranchEntity.name());
      assertThat(branch.address()).isEqualTo(testBranchEntity.address());
      assertThat(branch.phoneNumber()).isEqualTo(testBranchEntity.phoneNumber());
      assertThat(branch.openingTime()).isEqualTo(testBranchEntity.openingTime());
      assertThat(branch.closingTime()).isEqualTo(testBranchEntity.closingTime());
      assertThat(branch.maxConcurrentAppointmentsPerSlot()).isEqualTo(
          testBranchEntity.maxConcurrentAppointmentsPerSlot());
      verify(repository).findById(1L);
    }
  }

  @Nested
  @DisplayName("Search Branches Tests")
  class SearchBranchesTests {

    @Test
    @DisplayName("Should return branches matching search query by name")
    void shouldSearchByName() {
      // Given
      when(repository.searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of(testBranchEntity));

      // When
      Page<Branch> result = service.search("johannesburg", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).name()).contains("Johannesburg");
      verify(repository).searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should return branches matching search query by code")
    void shouldSearchByCode() {
      // Given
      when(repository.searchBranches("jhb-001", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of(testBranchEntity));

      // When
      Page<Branch> result = service.search("jhb-001", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).code()).isEqualTo("JHB-001");
      verify(repository).searchBranches("jhb-001", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should return branches matching search query by address")
    void shouldSearchByAddress() {
      // Given
      when(repository.searchBranches("main street", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of(testBranchEntity));

      // When
      Page<Branch> result = service.search("main street", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).address()).contains("Main Street");
      verify(repository).searchBranches("main street", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should return empty page when no matches found")
    void shouldReturnEmptyWhenNoMatches() {
      // Given
      when(repository.searchBranches("nonexistent", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of());

      // When
      Page<Branch> result = service.search("nonexistent", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verify(repository).searchBranches("nonexistent", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should return empty page when search query is empty")
    void shouldReturnEmptyWhenQueryEmpty() {
      // When
      Page<Branch> result = service.search("", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verify(repository, never()).searchBranches(anyString(), anyInt(),
          anyInt());
    }

    @Test
    @DisplayName("Should return empty page when search query is whitespace only")
    void shouldReturnEmptyWhenQueryWhitespaceOnly() {
      // Given

      // When
      Page<Branch> result = service.search("   ", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verify(repository, never()).searchBranches(anyString(), anyInt(),
          anyInt());
    }

    @Test
    @DisplayName("Should trim search query before searching")
    void shouldTrimSearchQuery() {
      // Given
      when(repository.searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of(testBranchEntity));

      // When
      Page<Branch> result = service.search("  johannesburg  ", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      verify(repository).searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should support pagination with search results")
    void shouldSupportPagination() {
      // Given
      Pageable customPageable = PageRequest.of(0, 5);
      BranchEntity branch2 = new BranchEntity(
          2L, "JHB-002", "Johannesburg North", "789 North Ave",
          "+27111234568", "jhb2@example.com", LocalTime.of(9, 0), LocalTime.of(17, 0), 3,
          true, Instant.now(), Instant.now()
      );
      when(repository.searchBranches("johannesburg", customPageable.getPageSize(),
          customPageable.getPageNumber())).thenReturn(
          List.of(testBranchEntity, branch2));

      // When
      Page<Branch> result = service.search("johannesburg", customPageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getNumber()).isEqualTo(0);
      assertThat(result.getSize()).isEqualTo(5);
      assertThat(result.getTotalElements()).isEqualTo(2);
      verify(repository).searchBranches("johannesburg", customPageable.getPageSize(),
          customPageable.getPageNumber());
    }

    @Test
    @DisplayName("Should return partial match results")
    void shouldReturnPartialMatches() {
      // Given
      when(repository.searchBranches("joh", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(List.of(testBranchEntity));

      // When
      Page<Branch> result = service.search("joh", pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      verify(repository).searchBranches("joh", pageable.getPageSize(), pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should convert search results to models")
    void shouldConvertSearchResultsToModels() {
      // Given
      BranchEntity branch2 = new BranchEntity(
          2L, "JHB-002", "Johannesburg North", "789 North Ave",
          "+27111234568", "jhb2@example.com", LocalTime.of(9, 0), LocalTime.of(17, 0), 3,
          true, Instant.now(), Instant.now()
      );
      when(repository.searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber())).thenReturn(
          List.of(testBranchEntity, branch2));

      // When
      Page<Branch> result = service.search("johannesburg", pageable);

      // Then
      assertThat(result.getContent()).allSatisfy(branch -> {
        assertThat(branch).isNotNull();
        assertThat(branch.id()).isNotNull();
        assertThat(branch.code()).isNotNull();
        assertThat(branch.name()).isNotNull();
        assertThat(branch.name().toLowerCase()).contains("johannesburg");
      });
      verify(repository).searchBranches("johannesburg", pageable.getPageSize(),
          pageable.getPageNumber());
    }

    @Test
    @DisplayName("Should handle multiple pages in search pagination")
    void shouldHandleMultiplePagesInSearch() {
      // Given
      Pageable firstPage = PageRequest.of(0, 2);
      BranchEntity branch2 = new BranchEntity(
          2L, "JHB-002", "Johannesburg North", "789 North Ave",
          "+27111234568", "jhb2@example.com", LocalTime.of(9, 0), LocalTime.of(17, 0), 3,
          true, Instant.now(), Instant.now()
      );
      BranchEntity branch3 = new BranchEntity(
          3L, "JHB-003", "Johannesburg East", "999 East St",
          "+27111234569", "jhb3@example.com", LocalTime.of(8, 0), LocalTime.of(18, 0), 2,
          true, Instant.now(), Instant.now()
      );
      when(repository.searchBranches("johannesburg", firstPage.getPageSize(),
          firstPage.getPageNumber()))
          .thenReturn(List.of(testBranchEntity, branch2, branch3));

      // When
      Page<Branch> result = service.search("johannesburg", firstPage);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getNumber()).isEqualTo(0);
      assertThat(result.getTotalElements()).isEqualTo(3);
      assertThat(result.getTotalPages()).isEqualTo(2);
      verify(repository).searchBranches("johannesburg", firstPage.getPageSize(),
          firstPage.getPageNumber());
    }
  }
}
