package com.shop.warehouse.service.impl;

import com.shop.warehouse.domain.Item;
import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.domain.StockTransaction;
import com.shop.warehouse.domain.TransactionType;
import com.shop.warehouse.dto.request.CreateVariantRequest;
import com.shop.warehouse.dto.request.UpdateVariantRequest;
import com.shop.warehouse.dto.response.VariantResponse;
import com.shop.warehouse.exception.DuplicateResourceException;
import com.shop.warehouse.exception.ResourceNotFoundException;
import com.shop.warehouse.repository.ItemRepository;
import com.shop.warehouse.repository.ItemVariantRepository;
import com.shop.warehouse.repository.StockRepository;
import com.shop.warehouse.repository.StockTransactionRepository;
import com.shop.warehouse.service.VariantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VariantServiceImpl implements VariantService {

    private final ItemRepository itemRepository;
    private final ItemVariantRepository variantRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public VariantServiceImpl(ItemRepository itemRepository,
                              ItemVariantRepository variantRepository,
                              StockRepository stockRepository,
                              StockTransactionRepository stockTransactionRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.stockRepository = stockRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Override
    public VariantResponse createVariant(Long itemId, CreateVariantRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item induk dengan ID " + itemId + " tidak ditemukan."));

        String sku = request.getSku().trim().toUpperCase();
        if (variantRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("SKU varian '" + sku + "' sudah terdaftar di sistem.");
        }

        ItemVariant variant = new ItemVariant(
                item,
                sku,
                request.getVariantName().trim(),
                request.getAttributesJson(),
                request.getPrice()
        );

        ItemVariant savedVariant = variantRepository.save(variant);

        int initialQty = request.getInitialStock() != null && request.getInitialStock() > 0 ? request.getInitialStock() : 0;
        Stock stock = new Stock(savedVariant, initialQty);
        stockRepository.save(stock);
        savedVariant.setStock(stock);

        if (initialQty > 0) {
            StockTransaction tx = new StockTransaction(
                    savedVariant,
                    TransactionType.INITIAL,
                    initialQty,
                    initialQty,
                    "INIT-" + sku,
                    "Inisialisasi stok awal varian"
            );
            stockTransactionRepository.save(tx);
        }

        return mapToVariantResponse(savedVariant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantResponse> getVariantsByItemId(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item dengan ID " + itemId + " tidak ditemukan.");
        }
        return variantRepository.findByItemId(itemId).stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VariantResponse getVariantById(Long id) {
        ItemVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + id + " tidak ditemukan."));
        return mapToVariantResponse(variant);
    }

    @Override
    public VariantResponse updateVariant(Long id, UpdateVariantRequest request) {
        ItemVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + id + " tidak ditemukan."));

        String sku = request.getSku().trim().toUpperCase();
        if (variantRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException("SKU varian '" + sku + "' sudah digunakan oleh varian lain.");
        }

        variant.setSku(sku);
        variant.setVariantName(request.getVariantName().trim());
        variant.setAttributesJson(request.getAttributesJson());
        variant.setPrice(request.getPrice());
        if (request.getIsActive() != null) {
            variant.setIsActive(request.getIsActive());
        }

        ItemVariant updated = variantRepository.save(variant);
        return mapToVariantResponse(updated);
    }

    @Override
    public void deleteVariant(Long id) {
        ItemVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + id + " tidak ditemukan."));
        variantRepository.delete(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantResponse> getAllVariants() {
        return variantRepository.findAll().stream()
                .map(this::mapToVariantResponse)
                .collect(Collectors.toList());
    }

    private VariantResponse mapToVariantResponse(ItemVariant variant) {
        VariantResponse res = new VariantResponse();
        res.setId(variant.getId());
        if (variant.getItem() != null) {
            res.setItemId(variant.getItem().getId());
            res.setItemCode(variant.getItem().getCode());
            res.setItemName(variant.getItem().getName());
        }
        res.setSku(variant.getSku());
        res.setVariantName(variant.getVariantName());
        res.setAttributesJson(variant.getAttributesJson());
        res.setPrice(variant.getPrice());
        res.setEffectivePrice(variant.getEffectivePrice());
        res.setIsActive(variant.getIsActive());
        res.setCreatedAt(variant.getCreatedAt());
        res.setUpdatedAt(variant.getUpdatedAt());

        int qty = 0;
        if (variant.getStock() != null) {
            qty = variant.getStock().getQuantity();
        } else {
            // Lazy load stock if null
            Stock s = stockRepository.findByVariantId(variant.getId()).orElse(null);
            if (s != null) {
                qty = s.getQuantity();
            }
        }
        res.setCurrentStock(qty);
        res.setIsAvailable(qty > 0);

        return res;
    }
}
