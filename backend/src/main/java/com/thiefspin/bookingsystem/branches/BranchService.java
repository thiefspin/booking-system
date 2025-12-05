package com.thiefspin.bookingsystem.branches;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BranchService {

  private BranchRepository repository;

  public Page<Branch> list(Pageable pageable) {
    return repository.findAll(pageable)
        .map(BranchEntity::toModel);
  }

  @Cacheable(value = "branches", key = "#id")
  public Optional<Branch> findById(Long id) {
    return repository.findById(id).map(BranchEntity::toModel);
  }

  public Page<Branch> search(String query, Pageable pageable) {
    String searchTerm = query.trim().toLowerCase();

    if (searchTerm.isEmpty()) {
      return Page.empty(pageable);
    }

    return createPage(
        () -> repository.searchBranches(searchTerm, pageable.getPageSize(), pageable.getOffset()),
        () -> repository.countSearchResults(searchTerm),
        pageable
    );
  }

  private Page<Branch> createPage(
      Supplier<List<BranchEntity>> contentSupplier,
      Supplier<Long> countSupplier,
      Pageable pageable
  ) {
    List<Branch> content = contentSupplier.get().stream()
        .map(BranchEntity::toModel)
        .toList();

    long total = countSupplier.get();

    return new PageImpl<>(content, pageable, total);
  }
}
