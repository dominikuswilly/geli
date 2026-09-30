package com.shop.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class StockAdjustRequest {

    @NotNull(message = "ID varian wajib diisi")
    private Long variantId;

    @NotNull(message = "Kuantitas aktual hasil opname wajib diisi")
    @PositiveOrZero(message = "Kuantitas fisik tidak boleh negatif")
    private Integer actualQuantity;

    @Size(max = 50, message = "Alasan penyesuaian maksimal 50 karakter")
    private String reason;

    private String notes;

    public StockAdjustRequest() {
    }

    public StockAdjustRequest(Long variantId, Integer actualQuantity, String reason, String notes) {
        this.variantId = variantId;
        this.actualQuantity = actualQuantity;
        this.reason = reason;
        this.notes = notes;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
