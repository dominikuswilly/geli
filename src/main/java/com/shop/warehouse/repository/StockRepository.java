package com.shop.warehouse.repository;

import com.shop.warehouse.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByVariantId(Long variantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Stock s SET s.quantity = s.quantity - :qty, s.updatedAt = :now " +
           "WHERE s.variant.id = :variantId AND s.quantity >= :qty")
    int deductStockAtomic(@Param("variantId") Long variantId,
                          @Param("qty") Integer qty,
                          @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Stock s SET s.quantity = s.quantity + :qty, s.updatedAt = :now " +
           "WHERE s.variant.id = :variantId")
    int addStockAtomic(@Param("variantId") Long variantId,
                       @Param("qty") Integer qty,
                       @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Stock s SET s.quantity = :newQty, s.updatedAt = :now " +
           "WHERE s.variant.id = :variantId")
    int setStockQuantity(@Param("variantId") Long variantId,
                         @Param("newQty") Integer newQty,
                         @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s")
    Long sumTotalStockQuantity();
}
