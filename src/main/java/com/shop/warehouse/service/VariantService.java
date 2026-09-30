package com.shop.warehouse.service;

import com.shop.warehouse.dto.request.CreateVariantRequest;
import com.shop.warehouse.dto.request.UpdateVariantRequest;
import com.shop.warehouse.dto.response.VariantResponse;

import java.util.List;

public interface VariantService {

    VariantResponse createVariant(Long itemId, CreateVariantRequest request);

    List<VariantResponse> getVariantsByItemId(Long itemId);

    VariantResponse getVariantById(Long id);

    VariantResponse updateVariant(Long id, UpdateVariantRequest request);

    void deleteVariant(Long id);

    List<VariantResponse> getAllVariants();
}
