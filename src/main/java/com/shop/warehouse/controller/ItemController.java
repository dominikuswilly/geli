package com.shop.warehouse.controller;

import com.shop.warehouse.dto.request.CreateItemRequest;
import com.shop.warehouse.dto.request.UpdateItemRequest;
import com.shop.warehouse.dto.response.ItemResponse;
import com.shop.warehouse.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "Item Management", description = "Endpoints untuk mengelola master data produk/barang induk")
@CrossOrigin(origins = "*")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @Operation(summary = "Membuat Item Baru", description = "Mendaftarkan produk barang induk baru dengan kode dan harga dasar")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse created = itemService.createItem(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Daftar Item", description = "Mengambil daftar seluruh item dengan dukungan filter pencarian dan paginasi")
    public ResponseEntity<Page<ItemResponse>> getAllItems(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<ItemResponse> items = itemService.getAllItems(query, pageRequest);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail Item", description = "Mengambil rincian data item lengkap beserta seluruh variannya")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        ItemResponse item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Perbarui Item", description = "Memperbarui data nama, deskripsi, kategori, atau harga dasar item")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemRequest request) {
        ItemResponse updated = itemService.updateItem(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus Item", description = "Menghapus produk item beserta variannya dari sistem")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
