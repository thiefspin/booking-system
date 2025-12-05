package com.thiefspin.bookingsystem.branches;

import com.thiefspin.bookingsystem.util.repository.BaseDataRepository;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends BaseDataRepository<BranchEntity, Long> {

  @Query(value = """
      SELECT * FROM booking.branches
      WHERE is_active = true
      AND (
          LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          OR LOWER(address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          OR LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
      )
      ORDER BY name
      LIMIT :limit OFFSET :offset
      """)
  List<BranchEntity> searchBranches(
      @Param("searchTerm") String searchTerm,
      @Param("limit") int limit,
      @Param("offset") long offset
  );

  @Query("""
      SELECT COUNT(*) FROM booking.branches
      WHERE is_active = true
      AND (
          LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          OR LOWER(address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          OR LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
      )
      """)
  long countSearchResults(@Param("searchTerm") String searchTerm);

}
