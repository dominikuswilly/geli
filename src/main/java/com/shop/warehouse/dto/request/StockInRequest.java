package com.shop.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class StockInRequest {

    @NotNull(message = "ID varian wajib diisi")
    private Long variantId;

    @NotNull(message = "Kuantitas penambahan wajib diisi")
    @Positive(message = "Kuantitas penambahan harus lebih besar dari 0")
    private Integer quantity;

    @Size(max = 100, message = "Nomor referensi maksimal 100 karakter")
    private String referenceNumber;

    private String notes;

    public StockInRequest() {
    }

    public StockInRequest(Long variantId, Integer quantity, String referenceNumber, String notes) {
        this.variantId = variantId;
        this.quantity = quantity;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
