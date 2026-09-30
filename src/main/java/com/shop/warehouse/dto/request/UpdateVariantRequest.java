package com.shop.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class UpdateVariantRequest {

    @NotBlank(message = "SKU varian tidak boleh kosong")
    @Size(max = 100, message = "SKU varian maksimal 100 karakter")
    private String sku;

    @NotBlank(message = "Nama varian tidak boleh kosong")
    @Size(max = 150, message = "Nama varian maksimal 150 karakter")
    private String variantName;

    private String attributesJson;

    @PositiveOrZero(message = "Harga varian tidak boleh negatif")
    private BigDecimal price;

    private Boolean isActive = true;

    public UpdateVariantRequest() {
    }

    public UpdateVariantRequest(String sku, String variantName, String attributesJson, BigDecimal price, Boolean isActive) {
        this.sku = sku;
        this.variantName = variantName;
        this.attributesJson = attributesJson;
        this.price = price;
        this.isActive = isActive != null ? isActive : true;
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

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
