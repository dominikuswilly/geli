package com.shop.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class UpdateItemRequest {

    @NotBlank(message = "Kode item tidak boleh kosong")
    @Size(max = 50, message = "Kode item maksimal 50 karakter")
    private String code;

    @NotBlank(message = "Nama item tidak boleh kosong")
    @Size(max = 200, message = "Nama item maksimal 200 karakter")
    private String name;

    private String description;

    @Size(max = 100, message = "Kategori maksimal 100 karakter")
    private String category;

    @NotNull(message = "Harga dasar wajib diisi")
    @PositiveOrZero(message = "Harga dasar tidak boleh negatif")
    private BigDecimal basePrice;

    public UpdateItemRequest() {
    }

    public UpdateItemRequest(String code, String name, String description, String category, BigDecimal basePrice) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
}
