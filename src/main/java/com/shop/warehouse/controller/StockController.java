package com.shop.warehouse.controller;

import com.shop.warehouse.dto.request.StockAdjustRequest;
import com.shop.warehouse.dto.request.StockDeductRequest;
import com.shop.warehouse.dto.request.StockInRequest;
import com.shop.warehouse.dto.response.StockMutationResponse;
import com.shop.warehouse.dto.response.StockResponse;
import com.shop.warehouse.dto.response.StockTransactionResponse;
import com.shop.warehouse.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stock & Inventory Operations", description = "Endpoints untuk mengelola stok persediaan dan proteksi anti-overselling")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Cek Saldo Stok Varian", description = "Mengambil data kuantitas stok saat ini dan status ketersediaannya")
    public ResponseEntity<StockResponse> getStockByVariantId(@PathVariable Long variantId) {
        StockResponse stock = stockService.getStockByVariantId(variantId);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/in")
    @Operation(summary = "Penambahan Stok (Restock / Stock-In)", description = "Menambahkan saldo fisik persediaan dari penerimaan barang pemasok")
    public ResponseEntity<StockMutationResponse> restock(@Valid @RequestBody StockInRequest request) {
        StockMutationResponse response = stockService.restock(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deduct")
    @Operation(summary = "Pemotongan Stok Penjualan (Anti-Overselling Guard)",
               description = "Memvalidasi ketersediaan dan memotong saldo stok secara atomik. Jika stok tidak mencukupi, mengembalikan HTTP 409 CONFLICT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stok berhasil dipotong"),
            @ApiResponse(responseCode = "409", description = "Stok tidak mencukupi atau habis (Overselling ditolak)")
    })
    public ResponseEntity<StockMutationResponse> deductStock(@Valid @RequestBody StockDeductRequest request) {
        StockMutationResponse response = stockService.deductStock(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/adjust")
    @Operation(summary = "Penyesuaian Stok (Stock Opname)", description = "Menyesuaikan kuantitas stok ke angka fisik gudang")
    public ResponseEntity<StockMutationResponse> adjustStock(@Valid @RequestBody StockAdjustRequest request) {
        StockMutationResponse response = stockService.adjustStock(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{variantId}")
    @Operation(summary = "Riwayat Mutasi Stok Varian", description = "Mengambil riwayat mutasi stok untuk satu varian spesifik")
    public ResponseEntity<List<StockTransactionResponse>> getStockHistory(@PathVariable Long variantId) {
        List<StockTransactionResponse> history = stockService.getStockHistoryByVariantId(variantId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history")
    @Operation(summary = "Seluruh Buku Besar Mutasi Stok", description = "Mengambil seluruh riwayat transaksi mutasi stok dengan paginasi")
    public ResponseEntity<Page<StockTransactionResponse>> getAllStockHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransactionResponse> history = stockService.getAllStockTransactions(pageRequest);
        return ResponseEntity.ok(history);
    }
}
