package com.shop.warehouse.repository;

import com.shop.warehouse.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("SELECT i FROM Item i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.code) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Item> searchItems(@Param("query") String query, Pageable pageable);
}
