package com.shop.warehouse.service;

import com.shop.warehouse.dto.request.CreateItemRequest;
import com.shop.warehouse.dto.request.UpdateItemRequest;
import com.shop.warehouse.dto.response.ItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {

    ItemResponse createItem(CreateItemRequest request);

    Page<ItemResponse> getAllItems(String query, Pageable pageable);

    ItemResponse getItemById(Long id);

    ItemResponse updateItem(Long id, UpdateItemRequest request);

    void deleteItem(Long id);
}
