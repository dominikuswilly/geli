package com.shop.warehouse.service.impl;

import com.shop.warehouse.domain.Item;
import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.domain.TransactionType;
import com.shop.warehouse.domain.StockTransaction;
import com.shop.warehouse.dto.request.CreateItemRequest;
import com.shop.warehouse.dto.request.UpdateItemRequest;
import com.shop.warehouse.dto.response.ItemResponse;
import com.shop.warehouse.dto.response.VariantResponse;
import com.shop.warehouse.exception.DuplicateResourceException;
import com.shop.warehouse.exception.ResourceNotFoundException;
import com.shop.warehouse.repository.ItemRepository;
import com.shop.warehouse.repository.StockRepository;
import com.shop.warehouse.repository.StockTransactionRepository;
import com.shop.warehouse.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public ItemServiceImpl(ItemRepository itemRepository,
                           StockRepository stockRepository,
                           StockTransactionRepository stockTransactionRepository) {
        this.itemRepository = itemRepository;
        this.stockRepository = stockRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Override
    public ItemResponse createItem(CreateItemRequest request) {
        if (itemRepository.existsByCode(request.getCode().trim())) {
            throw new DuplicateResourceException("Kode item '" + request.getCode() + "' sudah terdaftar di sistem.");
        }

        Item item = new Item(
                request.getCode().trim().toUpperCase(),
                request.getName().trim(),
                request.getDescription(),
                request.getCategory() != null ? request.getCategory().trim() : null,
                request.getBasePrice()
        );

        Item savedItem = itemRepository.save(item);
        return mapToItemResponse(savedItem, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponse> getAllItems(String query, Pageable pageable) {
        Page<Item> itemPage;
        if (query != null && !query.trim().isEmpty()) {
            itemPage = itemRepository.searchItems(query.trim(), pageable);
        } else {
            itemPage = itemRepository.findAll(pageable);
        }
        return itemPage.map(item -> mapToItemResponse(item, true));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item dengan ID " + id + " tidak ditemukan."));
        return mapToItemResponse(item, true);
    }

    @Override
    public ItemResponse updateItem(Long id, UpdateItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item dengan ID " + id + " tidak ditemukan."));

        if (itemRepository.existsByCodeAndIdNot(request.getCode().trim(), id)) {
            throw new DuplicateResourceException("Kode item '" + request.getCode() + "' sudah digunakan oleh item lain.");
        }

        item.setCode(request.getCode().trim().toUpperCase());
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        item.setBasePrice(request.getBasePrice());

        Item updated = itemRepository.save(item);
        return mapToItemResponse(updated, true);
    }

    @Override
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item dengan ID " + id + " tidak ditemukan."));
        itemRepository.delete(item);
    }

    private ItemResponse mapToItemResponse(Item item, boolean includeVariants) {
        ItemResponse response = new ItemResponse();
        response.setId(item.getId());
        response.setCode(item.getCode());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setCategory(item.getCategory());
        response.setBasePrice(item.getBasePrice());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());

        List<VariantResponse> variantResponses = new ArrayList<>();
        int totalStock = 0;
        if (item.getVariants() != null) {
            for (ItemVariant variant : item.getVariants()) {
                VariantResponse vr = new VariantResponse();
                vr.setId(variant.getId());
                vr.setItemId(item.getId());
                vr.setItemCode(item.getCode());
                vr.setItemName(item.getName());
                vr.setSku(variant.getSku());
                vr.setVariantName(variant.getVariantName());
                vr.setAttributesJson(variant.getAttributesJson());
                vr.setPrice(variant.getPrice());
                vr.setEffectivePrice(variant.getEffectivePrice());
                vr.setIsActive(variant.getIsActive());
                vr.setCreatedAt(variant.getCreatedAt());
                vr.setUpdatedAt(variant.getUpdatedAt());

                int stockQty = 0;
                if (variant.getStock() != null) {
                    stockQty = variant.getStock().getQuantity();
                }
                vr.setCurrentStock(stockQty);
                vr.setIsAvailable(stockQty > 0);

                totalStock += stockQty;
                if (includeVariants) {
                    variantResponses.add(vr);
                }
            }
        }

        response.setVariants(variantResponses);
        response.setVariantCount(item.getVariants() != null ? item.getVariants().size() : 0);
        response.setTotalStock(totalStock);

        return response;
    }
}
