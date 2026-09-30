package com.shop.warehouse.service;

import com.shop.warehouse.dto.request.StockAdjustRequest;
import com.shop.warehouse.dto.request.StockDeductRequest;
import com.shop.warehouse.dto.request.StockInRequest;
import com.shop.warehouse.dto.response.StockMutationResponse;
import com.shop.warehouse.dto.response.StockResponse;
import com.shop.warehouse.dto.response.StockTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockService {

    StockResponse getStockByVariantId(Long variantId);

    StockMutationResponse restock(StockInRequest request);

    StockMutationResponse deductStock(StockDeductRequest request);

    StockMutationResponse adjustStock(StockAdjustRequest request);

    List<StockTransactionResponse> getStockHistoryByVariantId(Long variantId);

    Page<StockTransactionResponse> getAllStockTransactions(Pageable pageable);
}
