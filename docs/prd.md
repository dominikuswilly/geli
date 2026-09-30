# Product Requirements Document (PRD)
## Sistem Manajemen Gudang Toko (Shop Warehouse Management System)

---

### Informasi Dokumen
- **Nama Produk:** Shop Warehouse Inventory Management API
- **Peran Penyusun:** System Analyst
- **Versi Produk:** v1.0.0
- **Tanggal:** 30 September 2026
- **Status:** Siap Implementasi Teknis

---

### 1. Gambaran Umum Produk (Product Overview)

Sistem Manajemen Gudang Toko adalah layanan *backend* berbasis RESTful API yang berfungsi sebagai mesin sentral pengelolaan inventaris toko ritel. Produk ini memungkinkan operator toko untuk membuat katalog barang (*items*), mengelompokkan varian barang (*variants* seperti ukuran dan warna), menetapkan strategi harga (*pricing*), serta melacak kuantitas persediaan stok secara terperinci.

Fitur paling krusial dari produk ini adalah mekanisme **Out-of-Stock Prevention Engine**, yang secara ketat dan atomik mencegah pengurangan stok melebihi saldo persediaan yang ada, menjaga kredibilitas operasional bisnis terhadap konsumen akhir.

---

### 2. Persona Pengguna (User Personas)

#### Persona 1: Budi Santoso (Admin Gudang)
- **Karakteristik:** Bertanggung jawab langsung atas bongkar muat barang masuk dan audit fisik barang di gudang.
- **Tujuan:** 
  - Mendaftarkan produk baru dan varian ukurannya dengan cepat.
  - Memperbarui kuantitas stok saat kiriman dari pemasok tiba (*restock*).
  - Melakukan penyesuaian jika ada barang rusak saat disimpan (*stock adjustment*).
- **Titik Frustrasi:** Bingung jika varian barang tidak memiliki SKU unik, atau sistem lambat saat memperbarui data puluhan varian.

#### Persona 2: Siti Rahma (Manajer Toko & Kasir)
- **Karakteristik:** Mengawasi operasional harian kasir dan katalog harga produk.
- **Tujuan:**
  - Memastikan harga di sistem selalu konsisten (harga dasar produk vs harga khusus varian).
  - Mengetahui secara seketika apakah suatu varian barang masih tersedia atau sudah kosong sebelum melayani pelanggan.
- **Titik Frustrasi:** Terjadi komplain pelanggan akibat kasir berhasil memproses transaksi barang yang ternyata fisiknya telah kosong (*overselling*).

#### Persona 3: Doni Pratama (Integrator Sistem / Client Application)
- **Karakteristik:** Pengembang aplikasi antarmuka Point of Sale (POS) atau platform web e-commerce.
- **Tujuan:**
  - Memanggil REST API yang terstandarisasi, memiliki validasi yang jelas, serta mengembalikan kode HTTP standar (200, 201, 400, 404, 409).
  - Mengandalkan jaminan integritas data dari API saat *checkout* penjualan secara bersamaan.

---

### 3. Ruang Lingkup Produk (Product Scope)

```
+---------------------------------------------------------------+
|                       SHOP WAREHOUSE API                      |
+-------------------------------+-------------------------------+
|       Katalog & Varian        |       Harga & Inventaris      |
+-------------------------------+-------------------------------+
| - Master Item (CRUD)          | - Base Price & Variant Price  |
| - Atribut Varian (CRUD)       | - Stock Tracking Realtime     |
| - SKU Unik Generator/Validator| - Stock In (Restock)          |
| - Filter & Pencarian          | - Out-of-Stock Guard Engine   |
+-------------------------------+-------------------------------+
```

- **Fitur Utama yang Disertakan (In-Scope):**
  1. Layanan CRUD lengkap untuk Item (Barang Induk).
  2. Layanan CRUD lengkap untuk Varian Barang.
  3. Konfigurasi penetapan harga hierarkis (Harga dasar & *Override* harga varian).
  4. Manajemen kuantitas stok persediaan per varian.
  5. API Pengurangan Stok (*Deduct Stock*) dengan validasi ketat pencegah *overselling*.
  6. Riwayat/Audit mutasi stok (*Stock Transaction Ledger*).

- **Batasan (Out-of-Scope untuk v1.0.0):**
  1. Otentikasi dan otorisasi multi-role (akan ditambahkan pada v2.0.0).
  2. Multi-lokasi gudang geografis (*single warehouse mode*).
  3. Antarmuka pengguna grafis (*pure API system*).

---

### 4. Epics, User Stories & Acceptance Criteria

#### Epic 1: Manajemen Master Data Item (Barang Induk)
Mengelola entitas dasar barang yang dijual di toko.

- **US-01: Pendaftaran Item Baru**
  - *Sebagai:* Admin Gudang
  - *Saya ingin:* Menambahkan data barang baru dengan nama, kode referensi, deskripsi, dan harga dasar
  - *Agar:* Barang tersebut terdaftar di dalam katalog sistem gudang.
  - *Kriteria Penerimaan (Acceptance Criteria):*
    - **Skenario 1 (Sukses):** Diberikan payload JSON valid dengan nama produk, kode barang unik, dan harga dasar non-negatif, sistem menyimpannya ke basis data dan merespons kode `201 Created` beserta ID barang yang dihasilkan.
    - **Skenario 2 (Gagal - Duplikasi):** Jika kode barang sudah ada di sistem, sistem menolak dan mengembalikan kode `400 Bad Request` dengan pesan "Kode barang sudah terdaftar".
    - **Skenario 3 (Gagal - Harga Negatif):** Jika harga dasar bernilai negatif, sistem menolak dengan pesan validasi yang sesuai.

- **US-02: Membaca & Memperbarui Item**
  - *Sebagai:* Manajer Toko
  - *Saya ingin:* Melihat daftar barang dan mengubah detail barang yang telah ada
  - *Agar:* Data katalog selalu mutakhir.
  - *Kriteria Penerimaan:*
    - Sistem menyediakan endpoint pencarian dengan paging dan penyaringan.
    - Pembaruan nama/deskripsi/harga pada barang mengembalikan data terbaru dengan kode `200 OK`.

---

#### Epic 2: Manajemen Varian Produk (Variants)
Mengelola turunan varian dari barang induk (seperti variasi ukuran, warna, tipe).

- **US-03: Penambahan Varian ke Item**
  - *Sebagai:* Admin Gudang
  - *Saya ingin:* Mendaftarkan satu atau lebih varian untuk item induk yang sudah terdaftar
  - *Agar:* Setiap opsi spesifik barang memiliki identifikasi SKU tersendiri.
  - *Kriteria Penerimaan:*
    - **Skenario 1 (Sukses):** Mengirimkan data varian dengan Item ID, SKU varian unik, atribut (cth: ukuran="L", warna="Hitam"), dan harga khusus (opsional). Sistem merespons `201 Created`.
    - **Skenario 2 (SKU Duplikat):** Jika SKU varian sudah ada di sistem (baik pada item yang sama maupun item lain), sistem merespons `400 Bad Request`.
    - **Skenario 3 (Item Tidak Ditemukan):** Jika Item ID tidak terdaftar, sistem merespons `404 Not Found`.

- **US-04: Pengelolaan Siklus Varian**
  - *Sebagai:* Admin Gudang
  - *Saya ingin:* Mengubah rincian varian atau menghapus varian yang sudah tidak diproduksi
  - *Agar:* Daftar varian tetap rapi dan relevan.

---

#### Epic 3: Manajemen Harga (Pricing Strategy)
Menentukan harga jual barang secara fleksibel.

- **US-05: Penentuan Harga Efektif Varian**
  - *Sebagai:* Kasir / Sistem Penjualan
  - *Saya ingin:* Mendapatkan harga efektif yang berlaku untuk suatu varian
  - *Agar:* Transaksi penjualan menggunakan harga yang akurat.
  - *Kriteria Penerimaan:*
    - **Skenario 1 (Harga Varian Ditetapkan):** Jika varian memiliki nilai `price` khusus (misal: IDR 120.000), maka sistem mengembalikan harga tersebut.
    - **Skenario 2 (Harga Varian Kosong / Fallback):** Jika nilai `price` pada varian adalah null, sistem secara otomatis menggunakan `basePrice` dari item induknya (misal: IDR 100.000).

---

#### Epic 4: Manajemen Stok & Pencegahan Overselling (Out-of-Stock Guard)
Melacak saldo fisik barang dan memvalidasi setiap pemotongan stok.

- **US-06: Penambahan Stok (Stock-In / Restock)**
  - *Sebagai:* Admin Gudang
  - *Saya ingin:* Menambahkan kuantitas stok untuk suatu varian
  - *Agar:* Saldo barang di sistem bertambah sesuai kiriman pemasok.
  - *Kriteria Penerimaan:*
    - Admin mengirimkan ID varian, kuantitas penambahan (> 0), dan referensi penerimaan.
    - Saldo stok bertambah secara atomik, riwayat mutasi tercatat, sistem mengembalikan saldo terbaru dengan kode `200 OK`.

- **US-07: Pengurangan Stok Penjualan (Stock Deduction / Checkout)**
  - *Sebagai:* Sistem Penjualan (Klien POS/E-commerce)
  - *Saya ingin:* Memotong kuantitas stok varian saat pelanggan melakukan transaksi pembelian
  - *Agar:* Kuantitas barang di gudang terpotong dan tidak terjadi penjualan ganda.
  - *Kriteria Penerimaan (Kritis):*
    - **Skenario 1 (Stok Cukup):** Saldo stok saat ini = 10 unit. Permintaan pengurangan = 3 unit. Sistem berhasil memotong stok menjadi 7 unit dan merespons `200 OK` dengan sisa saldo.
    - **Skenario 2 (Stok Tidak Cukup / Habis):** Saldo stok saat ini = 2 unit. Permintaan pengurangan = 5 unit. Sistem **menolak** transaksi, saldo tetap 2 unit, dan sistem merespons kode `409 Conflict` (atau `400 Bad Request`) dengan payload:
      ```json
      {
        "status": 409,
        "error": "INSUFFICIENT_STOCK",
        "message": "Stok tidak mencukupi untuk varian dengan SKU 'TSHIRT-BLK-L'. Stok tersedia: 2, diminta: 5."
      }
      ```
    - **Skenario 3 (Konkurensi Tinggi):** Jika 2 permintaan pemotongan masing-masing 5 unit datang secara simultan pada varian dengan sisa saldo 6 unit, tepat 1 transaksi akan berhasil (sisa stok 1 unit) dan 1 transaksi lainnya **wajib ditolak** dengan alasan stok tidak mencukupi.

---

### 5. Kebutuhan Non-Fungsional (Non-Functional Requirements)

1. **Konsistensi & Integritas Transaksi (Data Integrity):**
   - Operasi perubahan stok wajib terisolasi dalam transaksi database (*ACID Transaction*).
   - Pengurangan stok wajib dilindungi dengan mekanisme penanganan konkurensi (*Optimistic Locking* atau *Atomic Conditional Update* pada basis data).
2. **Kinerja (Performance):**
   - *Throughput* operasi baca (*read*) minimal 500 permintaan per detik.
   - Waktu respons pemotongan stok rata-rata di bawah 150 milidetik.
3. **Format Data & Kepatuhan Standar (API Standardization):**
   - Mengikuti kaidah RESTful API level 2 (*Richardson Maturity Model*).
   - Semua *payload* pertukaran data berformat `application/json; charset=utf-8`.
   - Format waktu menggunakan ISO-8601 dengan zona waktu Jakarta (`+07:00`).
4. **Keandalan & Ketahanan (Reliability):**
   - Penanganan pengecualian global (*Global Exception Handler*) untuk menghasilkan format respon error standar yang konsisten dan informatif bagi klien.

---

### 6. Rencana Rilis & Milestone

| Fase | Target Deliverable | Periode Waktu |
| :--- | :--- | :--- |
| **Milestone 1** | Finalisasi Dokumen Arsitektur & Kebutuhan (BRD, PRD, FSD, TSD) | Hari ke-1 |
| **Milestone 2** | Implementasi Domain Entity, Skema DB, dan Repositori JPA | Hari ke-2 |
| **Milestone 3** | Implementasi Service Layer, Validasi, dan Engine Pencegah Overselling | Hari ke-3 |
| **Milestone 4** | Implementasi REST Controller, DTO, Error Handler, dan Swagger OpenAPI | Hari ke-4 |
| **Milestone 5** | Pengujian Terintegrasi (Unit & Concurrency Tests) dan Dokumentasi Penggunaan | Hari ke-5 |
