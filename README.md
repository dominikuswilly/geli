# Shop Warehouse Management System with Anti-Overselling Guard

> Backend RESTful API & Frontend Dashboard modern untuk manajemen inventaris gudang toko ritel berbasis **Spring Boot 3.3.4** dan **Java 17 LTS**.

---

## 📋 Daftar Isi
1. [Gambaran Umum](#-gambaran-umum)
2. [Fitur Utama](#-fitur-utama)
3. [Keputusan Desain & Arsitektur (Design Decisions)](#-keputusan-desain--arsitektur-design-decisions)
4. [Asumsi yang Digunakan (Assumptions)](#-asumsi-yang-digunakan-assumptions)
5. [Spesifikasi Tumpukan Teknologi](#-spesifikasi-tumpukan-teknologi)
6. [Panduan Menjalankan Aplikasi (How to Run)](#-panduan-menjalankan-aplikasi-how-to-run)
7. [Contoh Penggunaan API Endpoints (API Examples)](#-contoh-penggunaan-api-endpoints-api-examples)
8. [Dokumentasi Spesifikasi Lengkap](#-dokumentasi-spesifikasi-lengkap)

---

## 🌟 Gambaran Umum

Sistem ini dirancang untuk mengatasi permasalahan klasik operasional gudang toko:
- **Pelacakan Varian Produk:** Mendukung barang induk (*Items*) yang memiliki beragam turunan varian (*Variants* seperti ukuran, warna, dsb.) dengan identifikasi SKU independen.
- **Strategi Penetapan Harga Hierarkis:** Menghitung harga efektif secara dinamis (menggunakan harga dasar produk induk atau harga *override* khusus varian).
- **Mesin Proteksi Anti-Overselling (Out-of-Stock Guard):** Mencegah penjualan barang yang stoknya nol atau kurang dari kuantitas permintaan, aman dari kondisi *race condition* pada akses bersamaan (*high concurrency*).
- **Frontend Dashboard Interaktif:** Antarmuka visual responsif berbasis SPA (*Single Page Application*) dengan dark-mode dan visualisasi langsung.
- **Data Sampel Terpasang (*Pre-seeded Data*):** Langsung siap diuji coba begitu aplikasi dijalankan.

---

## 🚀 Fitur Utama

- **Katalog Master Item & Varian (CRUD):** Tambah, lihat, ubah, dan hapus barang serta variannya.
- **Hierarchical Effective Pricing:** Otomatis mengambil `variant.price` jika ada, atau mundur ke `item.basePrice` jika kosong.
- **Anti-Overselling Engine:** Menggunakan operasi atomik bersyarat di level basis data (`WHERE quantity >= requestedQuantity`). Menolak seketika (`HTTP 409 Conflict`) jika stok tidak mencukupi.
- **Audit Mutasi Stok (Ledger):** Mencatat setiap histori mutasi barang (`INITIAL`, `RESTOCK`, `SALE_DEDUCT`, `ADJUSTMENT`).
- **Simulasi Kasir POS Interaktif:** Demo langsung pemotongan stok dan respons pencegahan *overselling*.
- **Swagger OpenAPI & H2 Database Console Terintegrasi.**

---

## 🧠 Keputusan Desain & Arsitektur (Design Decisions)

1. **Defense-in-Depth untuk Anti-Overselling (Pertahanan Berlapis):**
   - **Lapisan 1 (Atomic Conditional Update):** Operasi pemotongan stok dieksekusi langsung pada basis data menggunakan SQL atomik:
     ```sql
     UPDATE stocks SET quantity = quantity - :qty, updated_at = :now
     WHERE variant_id = :variantId AND quantity >= :qty;
     ```
     Jika baris yang terpengaruh bernilai `0`, sistem segera melempar `InsufficientStockException` (HTTP 409). Ini mengeliminasi *race condition* tanpa memerlukan *distributed lock* yang berat.
   - **Lapisan 2 (Check Constraint):** Kolom saldo `quantity` memiliki constraint `CHECK (quantity >= 0)`.
   - **Lapisan 3 (Optimistic Locking):** Entitas `Stock` dilengkapi anotasi `@Version` untuk mencegah *lost updates*.

2. **Pemisahan Entitas Item dan Varian:**
   - Produk induk (`Item`) menyimpan metadata umum (nama produk, kategori, harga dasar acuan).
   - Varian produk (`ItemVariant`) menyimpan atribut unik (SKU, nama varian, ukuran, warna, serta opsi harga khusus).
   - Saldo stok persediaan (`Stock`) diisolasi pada entitas tersendiri yang berelasi One-to-One dengan `ItemVariant`, mengoptimalkan performa kueri transaksi stok tanpa membebani tabel katalog utama.

3. **In-Memory H2 Database dengan Mode Kompatibilitas PostgreSQL:**
   - Memungkinkan aplikasi dapat dijalankan secara instan (*zero external dependency*) oleh siapa pun tanpa perlu instalasi server database eksternal terlebih dahulu.
   - Menggunakan konfigurasi `MODE=PostgreSQL` sehingga siap dialihkan ke database PostgreSQL skala produksi kapan saja hanya dengan mengubah koneksi `application.yml`.

4. **Arsitektur Antarmuka Single-Process (Embedded SPA):**
   - Antarmuka frontend (HTML/CSS/JS) disajikan langsung oleh Spring Boot melalui direktori `src/main/resources/static`.
   - Pengguna hanya perlu menjalankan **1 perintah tunggal** (`mvn spring-boot:run`) untuk menjalankan baik REST API backend maupun Frontend Dashboard di `http://localhost:8085`.

---

## 📌 Asumsi yang Digunakan (Assumptions)

1. Satuan kuantitas stok barang dicatat dalam bilangan bulat non-negatif (*integers*, misal: pcs/unit).
2. Mata uang acuan operasional adalah Rupiah (IDR).
3. Setiap varian wajib memiliki kode SKU yang unik secara global.
4. Pemotongan stok terjadi saat order diselesaikan di kasir (*checkout / sale deduction*).

---

## 🛠️ Spesifikasi Tumpukan Teknologi

- **Backend:** Java 17 LTS, Spring Boot 3.3.4
- **Persistence & ORM:** Spring Data JPA, Hibernate ORM 6.5
- **Database:** H2 Database (In-Memory Engine)
- **Validasi:** Jakarta Bean Validation (Hibernate Validator)
- **Dokumentasi API:** Springdoc OpenAPI 2.5 (Swagger UI)
- **Frontend:** Vanilla HTML5, Modern CSS (Glassmorphism & Dark Mode), Modern JavaScript (Fetch API, Async/Await)
- **Pengujian:** JUnit 5, Spring Boot Test, Multi-threaded Concurrency Test (`ExecutorService`, `CountDownLatch`)

---

## 🏃 Panduan Menjalankan Aplikasi (How to Run)

### Prasyarat
- Java JDK 17 atau lebih baru terpasang di sistem (`java -version`).
- Apache Maven 3.8+ terpasang di sistem (`mvn -version`).

### Langkah 1: Kloning Repositori & Masuk ke Direktori
```bash
git clone <url-repository>
cd geli
```

### Langkah 2: Jalankan Pengujian Otomatis
```bash
mvn test
```
*Seluruh pengujian unit dan uji beban konkurensi (mencegah overselling secara bersamaan) akan dieksekusi.*

### Langkah 3: Jalankan Aplikasi

**Opsi A: Menggunakan Maven Langsung**
```bash
mvn spring-boot:run
```

**Opsi B: Menggunakan Docker Compose (Platform linux/x86 atau linux/amd64)**
```bash
# Build dan jalankan container
docker compose up --build -d

# Memeriksa log aplikasi
docker compose logs -f

# Menghentikan container
docker compose down
```

Aplikasi akan aktif pada port `8085`.

### Langkah 4: Buka di Web Browser
- **Frontend Dashboard:** [http://localhost:8085](http://localhost:8085)
- **Swagger OpenAPI UI:** [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- **H2 Database Console:** [http://localhost:8085/h2-console](http://localhost:8085/h2-console)
  - JDBC URL: `jdbc:h2:mem:warehouse_db`
  - User Name: `sa`
  - Password: *(kosongkan)*

---

## 📡 Contoh Penggunaan API Endpoints (API Examples)

Base URL: `http://localhost:8085/api/v1`

### 1. Membuat Item Baru
- **Endpoint:** `POST /api/v1/items`
- **Request Body:**
```json
{
  "code": "JAKET-BMBR",
  "name": "Jaket Bomber Parasut Pria",
  "description": "Jaket bomber tahan angin bahan parasut taslan",
  "category": "Outerwear",
  "basePrice": 220000.00
}
```
- **Response (201 Created):**
```json
{
  "id": 5,
  "code": "JAKET-BMBR",
  "name": "Jaket Bomber Parasut Pria",
  "description": "Jaket bomber tahan angin bahan parasut taslan",
  "category": "Outerwear",
  "basePrice": 220000.00,
  "totalStock": 0,
  "variantCount": 0
}
```

---

### 2. Menambahkan Varian ke Item
- **Endpoint:** `POST /api/v1/items/5/variants`
- **Request Body:**
```json
{
  "sku": "BMBR-NVY-XL",
  "variantName": "Navy - XL",
  "attributesJson": "{\"color\":\"Navy\",\"size\":\"XL\"}",
  "price": 235000.00,
  "initialStock": 15
}
```
- **Response (201 Created):**
```json
{
  "id": 14,
  "itemId": 5,
  "sku": "BMBR-NVY-XL",
  "variantName": "Navy - XL",
  "effectivePrice": 235000.00,
  "currentStock": 15,
  "isAvailable": true
}
```

---

### 3. Mengurangi Stok Penjualan (Anti-Overselling Guard)
- **Endpoint:** `POST /api/v1/stocks/deduct`

**Kasus A: Kuantitas Cukup (Sukses)**
- **Request Body:**
```json
{
  "variantId": 14,
  "quantity": 3,
  "referenceNumber": "ORD-2026-0001",
  "notes": "Penjualan via POS"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "variantId": 14,
  "sku": "BMBR-NVY-XL",
  "operationType": "SALE_DEDUCT",
  "previousStock": 15,
  "changeQuantity": -3,
  "currentStock": 12,
  "referenceNumber": "ORD-2026-0001",
  "message": "Stok berhasil dipotong sebanyak 3 unit."
}
```

**Kasus B: Kuantitas Melebihi Stok (Ditolak Seketika)**
- **Request Body (Meminta 50 unit saat stok hanya tersisa 12 unit):**
```json
{
  "variantId": 14,
  "quantity": 50,
  "referenceNumber": "ORD-2026-0002"
}
```
- **Response (409 Conflict):**
```json
{
  "timestamp": "2026-09-30T18:45:00+07:00",
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Stok tidak mencukupi untuk varian dengan SKU 'BMBR-NVY-XL'. Stok tersedia: 12, diminta: 50.",
  "path": "/api/v1/stocks/deduct"
}
```

---

### 4. Menambah Stok (Restock)
- **Endpoint:** `POST /api/v1/stocks/in`
- **Request Body:**
```json
{
  "variantId": 14,
  "quantity": 20,
  "referenceNumber": "PO-SUPP-8899",
  "notes": "Penerimaan restock gudang"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "variantId": 14,
  "sku": "BMBR-NVY-XL",
  "operationType": "RESTOCK",
  "previousStock": 12,
  "changeQuantity": 20,
  "currentStock": 32,
  "message": "Stok berhasil ditambahkan sebanyak 20 unit."
}
```

---

### 5. Penyesuaian Stok Fisik (Stock Opname)
- **Endpoint:** `POST /api/v1/stocks/adjust`
- **Request Body:**
```json
{
  "variantId": 14,
  "actualQuantity": 30,
  "reason": "STOCK_OPNAME_BULANAN",
  "notes": "2 unit rusak saat handling"
}
```
- **Response (200 OK):**
```json
{
  "success": true,
  "variantId": 14,
  "sku": "BMBR-NVY-XL",
  "operationType": "ADJUSTMENT",
  "previousStock": 32,
  "changeQuantity": -2,
  "currentStock": 30,
  "message": "Penyesuaian stok berhasil disimpan. Selisih: -2"
}
```

---

## 📚 Dokumentasi Spesifikasi Lengkap

Dokumentasi arsitektur dan analisis kebutuhan sistem lengkap tersedia pada berkas berikut:
- **[docs/brd.md](file:///docs/brd.md)** : *Business Requirements Document*
- **[docs/prd.md](file:///docs/prd.md)** : *Product Requirements Document*
- **[docs/fsd.md](file:///docs/fsd.md)** : *Functional Specification Document*
- **[docs/tsd.md](file:///docs/tsd.md)** : *Technical Specification Document*
