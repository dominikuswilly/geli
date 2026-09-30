package com.shop.warehouse.repository;

import com.shop.warehouse.domain.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByVariantIdOrderByCreatedAtDesc(Long variantId);

    Page<StockTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
