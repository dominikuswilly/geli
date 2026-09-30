# Functional Specification Document (FSD)
## Sistem Manajemen Gudang Toko (Shop Warehouse Management System)

---

### Informasi Dokumen
- **Nama Modul:** RESTful API Backend Shop Warehouse
- **Peran Penyusun:** System Analyst
- **Versi Dokumen:** 1.0.0
- **Tanggal:** 30 September 2026
- **Status:** Spesifikasi Final

---

### 1. Arsitektur Fungsional (Functional Architecture)

Sistem terdiri atas 4 modul fungsional utama yang saling terhubung:
1. **Modul Master Item:** Mengelola data katalog barang induk, deskripsi, dan harga acuan.
2. **Modul Varian Produk:** Mengelola opsi fisik barang (ukuran, warna, SKU) dan *override* harga.
3. **Modul Inventaris & Stok:** Mengelola kuantitas fisik, pencatatan mutasi, dan saldo per varian.
4. **Modul Mesin Proteksi Overselling (Out-of-Stock Guard):** Memverifikasi dan memotong stok secara atomik untuk mencegah penjualan saat barang kosong.

```
                    +-----------------------------+
                    |    Klien (POS / E-Commerce) |
                    +--------------+--------------+
                                   | HTTP/JSON
                                   v
             +---------------------------------------------+
             |        Spring Boot REST API Gateway         |
             +---------------------------------------------+
               |                  |                     |
               v                  v                     v
       +---------------+  +---------------+     +---------------+
       |  Item Module  |  | Variant Module|     | Stock Module  |
       +-------+-------+  +-------+-------+     +-------+-------+
               |                  |                     |
               +------------------+                     |
                                  |                     v
                                  |            +------------------+
                                  +----------->| Anti-Overselling |
                                               |   Guard Engine   |
                                               +--------+---------+
                                                        |
                                                        v
                                               +------------------+
                                               |  Relational DB   |
                                               +------------------+
```

---

### 2. Logika Bisnis Rinci (Detailed Business Logic)

#### 2.1 Relasi Item dan Varian
- Satu `Item` dapat memiliki satu atau lebih `ItemVariant`.
- Setiap `Item` memiliki atribut: `id`, `code` (unik), `name`, `description`, `category`, `basePrice`, `createdAt`, `updatedAt`.
- Setiap `ItemVariant` memiliki atribut: `id`, `itemId` (FK), `sku` (unik), `variantName` (misal: "Ukuran XL - Merah"), `attributes` (JSON/Key-Value: `{"size": "XL", "color": "Red"}`), `price` (opsional), `isActive`.

#### 2.2 Aturan Penetapan Harga Efektif (Effective Price Calculation)
Algoritma penentuan harga jual per varian:
```
Fungsi getEffectivePrice(variant):
  JIKA variant.price != NULL DAN variant.price > 0:
    KEMBALIKAN variant.price
  LAINNYA:
    item = findItemById(variant.itemId)
    KEMBALIKAN item.basePrice
```

#### 2.3 Siklus Hidup dan Status Stok
1. **Inisialisasi Stok:** Saat varian pertama kali dibuat, entitas `Stock` dibuat dengan kuantitas awal `0` (atau sesuai input admin).
2. **Penerimaan Barang (Stock In / Restock):**
   - Menambahkan nilai kuantitas ke saldo stok aktif.
   - Mencatat transaksi tipe `RESTOCK` di tabel log mutasi.
3. **Pengeluaran Penjualan (Stock Deduction / Out-of-Stock Guard):**
   - Menerima `variantId`, `quantity`, dan `referenceNumber` (ID pesanan kasir).
   - Melakukan pengecekan ketersediaan: `availableQuantity >= requestedQuantity`.
   - Jika `false`: Lempar pengecualian `InsufficientStockException` (HTTP 409 Conflict). Transaksi dibatalkan (Rollback).
   - Jika `true`: Kurangi saldo secara atomik: `newQuantity = availableQuantity - requestedQuantity`.
   - Catat transaksi tipe `SALE_DEDUCT` di tabel log mutasi.
4. **Penyesuaian Stok (Stock Adjustment):**
   - Digunakan oleh admin gudang saat *stock opname*.
   - Saldo stok diperbarui ke nilai aktual fisik, dan selisihnya dicatat dengan tipe `ADJUSTMENT`.

---

### 3. Spesifikasi Kontrak RESTful API

#### Standar Header Permintaan & Respon
- `Content-Type: application/json`
- `Accept: application/json`

#### Standar Format Respon Error (RFC 7807 Terstandarisasi)
```json
{
  "timestamp": "2026-09-30T18:30:00+07:00",
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Stok tidak mencukupi untuk varian SKU: 'SHIRT-BLU-M'. Stok tersedia: 3, diminta: 5.",
  "path": "/api/v1/stocks/deduct"
}
```

---

#### 3.1 Endpoint Modul Item

##### 1. `POST /api/v1/items` - Membuat Item Baru
- **Deskripsi:** Mendaftarkan produk induk baru.
- **Request Body:**
  ```json
  {
    "code": "ITM-001",
    "name": "Kemeja Katun Polos Pria",
    "description": "Kemeja bahan katun premium lengan panjang",
    "category": "Pakaian Pria",
    "basePrice": 150000.00
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "id": 1,
    "code": "ITM-001",
    "name": "Kemeja Katun Polos Pria",
    "description": "Kemeja bahan katun premium lengan panjang",
    "category": "Pakaian Pria",
    "basePrice": 150000.00,
    "createdAt": "2026-09-30T18:00:00+07:00",
    "updatedAt": "2026-09-30T18:00:00+07:00"
  }
  ```

##### 2. `GET /api/v1/items` - Mendapatkan Daftar Item
- **Deskripsi:** Mengambil daftar barang induk dengan dukungan paginasi dan pencarian.
- **Query Parameters:**
  - `page` (default: 0)
  - `size` (default: 20)
  - `query` (opsional: filter nama atau kode barang)
- **Response (200 OK):**
  ```json
  {
    "content": [
      {
        "id": 1,
        "code": "ITM-001",
        "name": "Kemeja Katun Polos Pria",
        "category": "Pakaian Pria",
        "basePrice": 150000.00,
        "variantCount": 3
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

##### 3. `GET /api/v1/items/{id}` - Detail Item & Seluruh Varian
- **Deskripsi:** Mengambil rincian item lengkap dengan daftar semua varian yang dimilikinya.
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "code": "ITM-001",
    "name": "Kemeja Katun Polos Pria",
    "description": "Kemeja bahan katun premium lengan panjang",
    "category": "Pakaian Pria",
    "basePrice": 150000.00,
    "variants": [
      {
        "id": 101,
        "sku": "KEM-PUT-M",
        "variantName": "Putih - M",
        "price": null,
        "effectivePrice": 150000.00,
        "currentStock": 25
      },
      {
        "id": 102,
        "sku": "KEM-PUT-XL",
        "variantName": "Putih - XL",
        "price": 165000.00,
        "effectivePrice": 165000.00,
        "currentStock": 0
      }
    ]
  }
  ```

##### 4. `PUT /api/v1/items/{id}` - Memperbarui Data Item
- **Deskripsi:** Mengubah nama, kategori, deskripsi, atau harga dasar barang.
- **Response (200 OK):** Mengembalikan data item yang telah diperbarui.

##### 5. `DELETE /api/v1/items/{id}` - Menghapus Item
- **Deskripsi:** Menghapus item jika belum memiliki riwayat transaksi, atau menonaktifkan (*soft-delete*).
- **Response (204 No Content)**.

---

#### 3.2 Endpoint Modul Varian

##### 1. `POST /api/v1/items/{itemId}/variants` - Membuat Varian Baru
- **Deskripsi:** Mendaftarkan varian baru pada item yang ditentukan.
- **Request Body:**
  ```json
  {
    "sku": "KEM-PUT-L",
    "variantName": "Putih - L",
    "attributes": {
      "color": "Putih",
      "size": "L"
    },
    "price": 150000.00,
    "initialStock": 10
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "id": 103,
    "itemId": 1,
    "sku": "KEM-PUT-L",
    "variantName": "Putih - L",
    "attributes": {
      "color": "Putih",
      "size": "L"
    },
    "price": 150000.00,
    "effectivePrice": 150000.00,
    "currentStock": 10,
    "isActive": true
  }
  ```

##### 2. `PUT /api/v1/variants/{id}` - Memperbarui Data Varian
- **Deskripsi:** Memperbarui nama varian, atribut, atau harga khusus varian.
- **Response (200 OK)**.

##### 3. `DELETE /api/v1/variants/{id}` - Menghapus Varian
- **Response (204 No Content)**.

---

#### 3.3 Endpoint Modul Stok & Pencegahan Overselling

##### 1. `GET /api/v1/stocks/variants/{variantId}` - Cek Ketersediaan Stok
- **Deskripsi:** Mengambil informasi saldo stok dan status ketersediaan.
- **Response (200 OK):**
  ```json
  {
    "variantId": 101,
    "sku": "KEM-PUT-M",
    "quantity": 25,
    "isAvailable": true,
    "status": "IN_STOCK",
    "lastUpdated": "2026-09-30T18:15:00+07:00"
  }
  ```

##### 2. `POST /api/v1/stocks/in` - Menambah Stok (Restock / Stock-In)
- **Deskripsi:** Mencatat penambahan kuantitas barang masuk.
- **Request Body:**
  ```json
  {
    "variantId": 101,
    "quantity": 50,
    "referenceNumber": "PO-SUPPLIER-202609-088",
    "notes": "Penerimaan PO kain katun batch 2"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "variantId": 101,
    "sku": "KEM-PUT-M",
    "previousStock": 25,
    "addedQuantity": 50,
    "currentStock": 75,
    "referenceNumber": "PO-SUPPLIER-202609-088",
    "transactionTime": "2026-09-30T18:20:00+07:00"
  }
  ```

##### 3. `POST /api/v1/stocks/deduct` - Memotong Stok Penjualan (Anti-Overselling Guard)
- **Deskripsi:** Memvalidasi ketersediaan dan memotong stok. Jika stok tidak mencukupi, permintaan **ditolak seketika**.
- **Request Body:**
  ```json
  {
    "variantId": 101,
    "quantity": 3,
    "referenceNumber": "ORD-20260930-0012",
    "notes": "Penjualan kasir counter 1"
  }
  ```
- **Kasus Sukses (Response 200 OK):**
  ```json
  {
    "success": true,
    "variantId": 101,
    "sku": "KEM-PUT-M",
    "deductedQuantity": 3,
    "remainingStock": 72,
    "referenceNumber": "ORD-20260930-0012",
    "transactionTime": "2026-09-30T18:25:00+07:00"
  }
  ```
- **Kasus Stok Tidak Cukup (Response 409 Conflict):**
  ```json
  {
    "timestamp": "2026-09-30T18:25:05+07:00",
    "status": 409,
    "error": "INSUFFICIENT_STOCK",
    "message": "Gagal memotong stok: Kuantitas tersedia (2) tidak mencukupi untuk permintaan (5) pada SKU: 'KEM-PUT-M'.",
    "path": "/api/v1/stocks/deduct"
  }
  ```

##### 4. `POST /api/v1/stocks/adjust` - Penyesuaian Stok (Stock Opname)
- **Deskripsi:** Menyesuaikan stok ke angka riil gudang dengan alasan selisih.
- **Request Body:**
  ```json
  {
    "variantId": 101,
    "actualQuantity": 70,
    "reason": "STOCK_OPNAME",
    "notes": "Ditemukan 2 unit cacat jahitan saat audit fisik"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "variantId": 101,
    "previousStock": 72,
    "adjustedStock": 70,
    "difference": -2,
    "reason": "STOCK_OPNAME",
    "updatedAt": "2026-09-30T18:30:00+07:00"
  }
  ```

---

### 4. Matriks Validasi Masukan (Input Validation Matrix)

| Entitas / Bidang | Validasi Teknis | Pesan Kesalahan |
| :--- | :--- | :--- |
| `item.code` | `@NotBlank`, Maksimal 50 Karakter, Huruf/Angka/Strip, Unik | Kode barang wajib diisi dan unik. |
| `item.name` | `@NotBlank`, Maksimal 200 Karakter | Nama barang wajib diisi. |
| `item.basePrice` | `@NotNull`, `@PositiveOrZero` | Harga dasar harus berupa angka non-negatif. |
| `variant.sku` | `@NotBlank`, Maksimal 100 Karakter, Unik Global | SKU varian wajib diisi dan belum digunakan. |
| `variant.price` | Opsional, jika diisi `@PositiveOrZero` | Harga varian harus bernilai non-negatif. |
| `stock.quantity` | `@NotNull`, `@Positive` (> 0 untuk mutasi) | Kuantitas mutasi harus berupa bilangan bulat positif. |
| `stock.deduct` | Kuantitas diminta <= Saldo stok saat ini | Stok tidak mencukupi (HTTP 409 Conflict). |
