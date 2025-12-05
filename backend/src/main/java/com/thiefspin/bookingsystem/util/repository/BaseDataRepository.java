package com.thiefspin.bookingsystem.util.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface BaseDataRepository<E, I> extends CrudRepository<E, I>,
    PagingAndSortingRepository<E, I> {

}
