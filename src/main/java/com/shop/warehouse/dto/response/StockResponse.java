package com.shop.warehouse.dto.response;

import java.time.LocalDateTime;

public class StockResponse {

    private Long variantId;
    private String sku;
    private String variantName;
    private Integer quantity;
    private Boolean isAvailable;
    private String status; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private LocalDateTime lastUpdated;

    public StockResponse() {
    }

    public StockResponse(Long variantId, String sku, String variantName, Integer quantity, LocalDateTime lastUpdated) {
        this.variantId = variantId;
        this.sku = sku;
        this.variantName = variantName;
        this.quantity = quantity;
        this.isAvailable = quantity != null && quantity > 0;
        if (quantity == null || quantity <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (quantity <= 5) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "IN_STOCK";
        }
        this.lastUpdated = lastUpdated;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.isAvailable = quantity != null && quantity > 0;
        if (quantity == null || quantity <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (quantity <= 5) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "IN_STOCK";
        }
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
