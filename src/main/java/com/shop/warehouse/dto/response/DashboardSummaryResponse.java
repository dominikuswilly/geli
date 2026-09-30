package com.shop.warehouse.dto.response;

import java.math.BigDecimal;

public class DashboardSummaryResponse {

    private long totalItems;
    private long totalVariants;
    private long totalStockQuantity;
    private BigDecimal totalValuation;
    private long outOfStockCount;
    private long lowStockCount;
    private long inStockCount;

    public DashboardSummaryResponse() {
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public long getTotalVariants() {
        return totalVariants;
    }

    public void setTotalVariants(long totalVariants) {
        this.totalVariants = totalVariants;
    }

    public long getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public void setTotalStockQuantity(long totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }

    public BigDecimal getTotalValuation() {
        return totalValuation;
    }

    public void setTotalValuation(BigDecimal totalValuation) {
        this.totalValuation = totalValuation;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getInStockCount() {
        return inStockCount;
    }

    public void setInStockCount(long inStockCount) {
        this.inStockCount = inStockCount;
    }
}
