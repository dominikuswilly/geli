package com.shop.warehouse.service.impl;

import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.domain.StockTransaction;
import com.shop.warehouse.domain.TransactionType;
import com.shop.warehouse.dto.request.StockAdjustRequest;
import com.shop.warehouse.dto.request.StockDeductRequest;
import com.shop.warehouse.dto.request.StockInRequest;
import com.shop.warehouse.dto.response.StockMutationResponse;
import com.shop.warehouse.dto.response.StockResponse;
import com.shop.warehouse.dto.response.StockTransactionResponse;
import com.shop.warehouse.exception.InsufficientStockException;
import com.shop.warehouse.exception.InvalidStockOperationException;
import com.shop.warehouse.exception.ResourceNotFoundException;
import com.shop.warehouse.repository.ItemVariantRepository;
import com.shop.warehouse.repository.StockRepository;
import com.shop.warehouse.repository.StockTransactionRepository;
import com.shop.warehouse.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private final ItemVariantRepository variantRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public StockServiceImpl(ItemVariantRepository variantRepository,
                            StockRepository stockRepository,
                            StockTransactionRepository stockTransactionRepository) {
        this.variantRepository = variantRepository;
        this.stockRepository = stockRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponse getStockByVariantId(Long variantId) {
        ItemVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + variantId + " tidak ditemukan."));

        Stock stock = stockRepository.findByVariantId(variantId)
                .orElseGet(() -> new Stock(variant, 0));

        return new StockResponse(
                variant.getId(),
                variant.getSku(),
                variant.getVariantName(),
                stock.getQuantity(),
                stock.getUpdatedAt() != null ? stock.getUpdatedAt() : LocalDateTime.now()
        );
    }

    @Override
    public StockMutationResponse restock(StockInRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidStockOperationException("Kuantitas penambahan stok harus lebih besar dari 0.");
        }

        ItemVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + request.getVariantId() + " tidak ditemukan."));

        Stock stock = stockRepository.findByVariantId(variant.getId())
                .orElseGet(() -> stockRepository.save(new Stock(variant, 0)));

        int prevStock = stock.getQuantity();
        LocalDateTime now = LocalDateTime.now();

        stockRepository.addStockAtomic(variant.getId(), request.getQuantity(), now);

        Stock updatedStock = stockRepository.findByVariantId(variant.getId()).orElseThrow();
        int newBalance = updatedStock.getQuantity();

        StockTransaction tx = new StockTransaction(
                variant,
                TransactionType.RESTOCK,
                request.getQuantity(),
                newBalance,
                request.getReferenceNumber(),
                request.getNotes() != null ? request.getNotes() : "Penambahan stok masuk (restock)"
        );
        stockTransactionRepository.save(tx);

        StockMutationResponse response = new StockMutationResponse();
        response.setSuccess(true);
        response.setVariantId(variant.getId());
        response.setSku(variant.getSku());
        response.setVariantName(variant.getVariantName());
        response.setOperationType("RESTOCK");
        response.setPreviousStock(prevStock);
        response.setChangeQuantity(request.getQuantity());
        response.setCurrentStock(newBalance);
        response.setReferenceNumber(request.getReferenceNumber());
        response.setMessage("Stok berhasil ditambahkan sebanyak " + request.getQuantity() + " unit.");
        response.setTransactionTime(now);

        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StockMutationResponse deductStock(StockDeductRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidStockOperationException("Kuantitas pemotongan stok harus lebih besar dari 0.");
        }

        ItemVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + request.getVariantId() + " tidak ditemukan."));

        Stock stock = stockRepository.findByVariantId(variant.getId())
                .orElseThrow(() -> new InsufficientStockException("Data stok tidak ditemukan untuk varian SKU: " + variant.getSku()));

        int prevStock = stock.getQuantity();
        LocalDateTime now = LocalDateTime.now();

        // ATOMIC CONDITIONAL UPDATE: Primary Defense against Overselling & Race Conditions
        int updated = stockRepository.deductStockAtomic(variant.getId(), request.getQuantity(), now);

        if (updated == 0) {
            // Race condition caught or stock insufficient!
            Stock currentStockState = stockRepository.findByVariantId(variant.getId()).orElse(stock);
            throw new InsufficientStockException(
                    "Stok tidak mencukupi untuk varian dengan SKU '" + variant.getSku() +
                    "'. Stok tersedia: " + currentStockState.getQuantity() +
                    ", diminta: " + request.getQuantity() + "."
            );
        }

        Stock updatedStock = stockRepository.findByVariantId(variant.getId()).orElseThrow();
        int newBalance = updatedStock.getQuantity();

        StockTransaction tx = new StockTransaction(
                variant,
                TransactionType.SALE_DEDUCT,
                -request.getQuantity(),
                newBalance,
                request.getReferenceNumber(),
                request.getNotes() != null ? request.getNotes() : "Pemotongan stok transaksi penjualan"
        );
        stockTransactionRepository.save(tx);

        StockMutationResponse response = new StockMutationResponse();
        response.setSuccess(true);
        response.setVariantId(variant.getId());
        response.setSku(variant.getSku());
        response.setVariantName(variant.getVariantName());
        response.setOperationType("SALE_DEDUCT");
        response.setPreviousStock(prevStock);
        response.setChangeQuantity(-request.getQuantity());
        response.setCurrentStock(newBalance);
        response.setReferenceNumber(request.getReferenceNumber());
        response.setMessage("Stok berhasil dipotong sebanyak " + request.getQuantity() + " unit.");
        response.setTransactionTime(now);

        return response;
    }

    @Override
    public StockMutationResponse adjustStock(StockAdjustRequest request) {
        if (request.getActualQuantity() < 0) {
            throw new InvalidStockOperationException("Kuantitas fisik tidak boleh kurang dari 0.");
        }

        ItemVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Varian dengan ID " + request.getVariantId() + " tidak ditemukan."));

        Stock stock = stockRepository.findByVariantId(variant.getId())
                .orElseGet(() -> stockRepository.save(new Stock(variant, 0)));

        int prevStock = stock.getQuantity();
        int diff = request.getActualQuantity() - prevStock;
        LocalDateTime now = LocalDateTime.now();

        stockRepository.setStockQuantity(variant.getId(), request.getActualQuantity(), now);

        StockTransaction tx = new StockTransaction(
                variant,
                TransactionType.ADJUSTMENT,
                diff,
                request.getActualQuantity(),
                request.getReason() != null ? request.getReason() : "STOCK_OPNAME",
                request.getNotes() != null ? request.getNotes() : "Penyesuaian stok opname fisik gudang"
        );
        stockTransactionRepository.save(tx);

        StockMutationResponse response = new StockMutationResponse();
        response.setSuccess(true);
        response.setVariantId(variant.getId());
        response.setSku(variant.getSku());
        response.setVariantName(variant.getVariantName());
        response.setOperationType("ADJUSTMENT");
        response.setPreviousStock(prevStock);
        response.setChangeQuantity(diff);
        response.setCurrentStock(request.getActualQuantity());
        response.setReferenceNumber(request.getReason());
        response.setMessage("Penyesuaian stok berhasil disimpan. Selisih: " + (diff >= 0 ? "+" + diff : diff));
        response.setTransactionTime(now);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getStockHistoryByVariantId(Long variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Varian dengan ID " + variantId + " tidak ditemukan.");
        }

        return stockTransactionRepository.findByVariantIdOrderByCreatedAtDesc(variantId).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> getAllStockTransactions(Pageable pageable) {
        return stockTransactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToTransactionResponse);
    }

    private StockTransactionResponse mapToTransactionResponse(StockTransaction tx) {
        StockTransactionResponse res = new StockTransactionResponse();
        res.setId(tx.getId());
        if (tx.getVariant() != null) {
            res.setVariantId(tx.getVariant().getId());
            res.setSku(tx.getVariant().getSku());
            res.setVariantName(tx.getVariant().getVariantName());
            if (tx.getVariant().getItem() != null) {
                res.setItemName(tx.getVariant().getItem().getName());
            }
        }
        res.setTrxType(tx.getTrxType().name());
        res.setQuantityChange(tx.getQuantityChange());
        res.setBalanceAfter(tx.getBalanceAfter());
        res.setReferenceNo(tx.getReferenceNo());
        res.setNotes(tx.getNotes());
        res.setCreatedAt(tx.getCreatedAt());
        return res;
    }
}
