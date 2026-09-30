package com.shop.warehouse.service;

import com.shop.warehouse.domain.Item;
import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.dto.request.StockAdjustRequest;
import com.shop.warehouse.dto.request.StockDeductRequest;
import com.shop.warehouse.dto.request.StockInRequest;
import com.shop.warehouse.dto.response.StockMutationResponse;
import com.shop.warehouse.dto.response.StockResponse;
import com.shop.warehouse.exception.InsufficientStockException;
import com.shop.warehouse.repository.ItemRepository;
import com.shop.warehouse.repository.ItemVariantRepository;
import com.shop.warehouse.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemVariantRepository variantRepository;

    @Autowired
    private StockRepository stockRepository;

    private ItemVariant testVariant;

    @BeforeEach
    void setUp() {
        Item item = new Item("TEST-ITEM-" + System.currentTimeMillis(), "Item Uji Coba", "Deskripsi", "Uji", new BigDecimal("50000.00"));
        item = itemRepository.save(item);

        testVariant = new ItemVariant(item, "TEST-SKU-" + System.currentTimeMillis(), "Varian Uji", "{}", new BigDecimal("55000.00"));
        testVariant = variantRepository.save(testVariant);

        Stock stock = new Stock(testVariant, 10);
        stockRepository.save(stock);
        testVariant.setStock(stock);
    }

    @Test
    @DisplayName("Cek Saldo Stok Mengembalikan Data Akurat")
    void testGetStock() {
        StockResponse response = stockService.getStockByVariantId(testVariant.getId());
        assertNotNull(response);
        assertEquals(10, response.getQuantity());
        assertTrue(response.getIsAvailable());
        assertEquals("IN_STOCK", response.getStatus());
    }

    @Test
    @DisplayName("Penambahan Stok (Restock) Berhasil Meningkatkan Saldo")
    void testRestockSuccess() {
        StockInRequest inReq = new StockInRequest(testVariant.getId(), 15, "PO-TEST-001", "Penerimaan barang tes");
        StockMutationResponse res = stockService.restock(inReq);

        assertTrue(res.getSuccess());
        assertEquals(10, res.getPreviousStock());
        assertEquals(15, res.getChangeQuantity());
        assertEquals(25, res.getCurrentStock());

        Stock stockInDb = stockRepository.findByVariantId(testVariant.getId()).orElseThrow();
        assertEquals(25, stockInDb.getQuantity());
    }

    @Test
    @DisplayName("Pemotongan Stok Berhasil Ketika Stok Mencukupi")
    void testDeductStockSuccess() {
        StockDeductRequest deductReq = new StockDeductRequest(testVariant.getId(), 4, "ORD-TEST-001", "Penjualan tes");
        StockMutationResponse res = stockService.deductStock(deductReq);

        assertTrue(res.getSuccess());
        assertEquals(10, res.getPreviousStock());
        assertEquals(-4, res.getChangeQuantity());
        assertEquals(6, res.getCurrentStock());

        Stock stockInDb = stockRepository.findByVariantId(testVariant.getId()).orElseThrow();
        assertEquals(6, stockInDb.getQuantity());
    }

    @Test
    @DisplayName("Anti-Overselling Guard: Melempar InsufficientStockException Ketika Permintaan Melebihi Stok")
    void testAntiOversellingGuardBlocksExceedingDeduction() {
        // Saldo awal = 10, minta 15
        StockDeductRequest deductReq = new StockDeductRequest(testVariant.getId(), 15, "ORD-TEST-EXCEED", "Penjualan melebihi stok");

        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            stockService.deductStock(deductReq);
        });

        assertTrue(exception.getMessage().contains("tidak mencukupi"));

        // Verifikasi saldo di DB tidak berubah (tetap 10)
        Stock stockInDb = stockRepository.findByVariantId(testVariant.getId()).orElseThrow();
        assertEquals(10, stockInDb.getQuantity());
    }

    @Test
    @DisplayName("Penyesuaian Stok (Stock Opname) Berhasil Menyimpan Angka Fisik")
    void testStockAdjustment() {
        StockAdjustRequest adjustReq = new StockAdjustRequest(testVariant.getId(), 8, "STOCK_OPNAME", "Audit barang rusak 2 pcs");
        StockMutationResponse res = stockService.adjustStock(adjustReq);

        assertTrue(res.getSuccess());
        assertEquals(10, res.getPreviousStock());
        assertEquals(-2, res.getChangeQuantity());
        assertEquals(8, res.getCurrentStock());

        Stock stockInDb = stockRepository.findByVariantId(testVariant.getId()).orElseThrow();
        assertEquals(8, stockInDb.getQuantity());
    }

    @Test
    @DisplayName("Uji Konkurensi: Mencegah Race Condition Saat Pengurangan Stok Simultan")
    void testConcurrentDeductionPreventsOverselling() throws InterruptedException {
        // Buat varian khusus dengan stok tepat 5 unit
        Item item = itemRepository.save(new Item("RACE-ITM-" + System.currentTimeMillis(), "Item Race", "", "", new BigDecimal("10000.00")));
        ItemVariant raceVariant = variantRepository.save(new ItemVariant(item, "RACE-SKU-" + System.currentTimeMillis(), "Race Var", "{}", null));
        stockRepository.save(new Stock(raceVariant, 5));

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 10 thread simultan masing-masing mencoba memotong 1 unit stok (total diminta = 10, stok hanya ada 5)
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // sinkronkan awal eksekusi bersamaan
                    StockDeductRequest req = new StockDeductRequest(
                            raceVariant.getId(), 1, "ORD-RACE-" + threadNum, "Uji konkurensi thread " + threadNum
                    );
                    stockService.deductStock(req);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failedCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Mulai serentak
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Semua thread pengujian konkurensi harus selesai dalam 5 detik");
        assertEquals(5, successCount.get(), "Hanya tepat 5 transaksi yang boleh berhasil");
        assertEquals(5, failedCount.get(), "Tepat 5 transaksi lainnya wajib ditolak karena stok habis");

        Stock finalStock = stockRepository.findByVariantId(raceVariant.getId()).orElseThrow();
        assertEquals(0, finalStock.getQuantity(), "Saldo akhir stok wajib bernilai tepat 0, tidak boleh negatif");
    }
}
