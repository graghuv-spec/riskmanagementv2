package com.riskmanagement.repository;

import com.riskmanagement.model.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
	@Query("select distinct b.businessSector from Borrower b where b.businessSector is not null and b.businessSector <> '' order by b.businessSector")
	List<String> findDistinctBusinessSectors();

	@Query("select distinct b.location from Borrower b where b.location is not null and b.location <> '' order by b.location")
	List<String> findDistinctLocations();
}