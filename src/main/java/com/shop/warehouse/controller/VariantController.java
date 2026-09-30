package com.shop.warehouse.controller;

import com.shop.warehouse.dto.request.CreateVariantRequest;
import com.shop.warehouse.dto.request.UpdateVariantRequest;
import com.shop.warehouse.dto.response.VariantResponse;
import com.shop.warehouse.service.VariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Variant Management", description = "Endpoints untuk mengelola data varian produk, SKU, dan penetapan harga khusus")
@CrossOrigin(origins = "*")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping("/api/v1/items/{itemId}/variants")
    @Operation(summary = "Tambah Varian Baru", description = "Mendaftarkan varian baru pada item induk tertentu")
    public ResponseEntity<VariantResponse> createVariant(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse created = variantService.createVariant(itemId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/api/v1/items/{itemId}/variants")
    @Operation(summary = "Daftar Varian per Item", description = "Mengambil seluruh varian yang dimiliki oleh suatu item induk")
    public ResponseEntity<List<VariantResponse>> getVariantsByItem(@PathVariable Long itemId) {
        List<VariantResponse> list = variantService.getVariantsByItemId(itemId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/v1/variants")
    @Operation(summary = "Daftar Seluruh Varian", description = "Mengambil seluruh varian yang ada di sistem")
    public ResponseEntity<List<VariantResponse>> getAllVariants() {
        List<VariantResponse> list = variantService.getAllVariants();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/v1/variants/{id}")
    @Operation(summary = "Detail Varian", description = "Mengambil rincian data varian, harga efektif, dan status stok")
    public ResponseEntity<VariantResponse> getVariantById(@PathVariable Long id) {
        VariantResponse variant = variantService.getVariantById(id);
        return ResponseEntity.ok(variant);
    }

    @PutMapping("/api/v1/variants/{id}")
    @Operation(summary = "Perbarui Varian", description = "Memperbarui data SKU, nama, atribut, atau harga khusus varian")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVariantRequest request) {
        VariantResponse updated = variantService.updateVariant(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/v1/variants/{id}")
    @Operation(summary = "Hapus Varian", description = "Menghapus varian barang dari sistem")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        variantService.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }
}
