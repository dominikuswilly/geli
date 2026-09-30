# Technical Specification Document (TSD)
## Sistem Manajemen Gudang Toko (Shop Warehouse Management System)

---

### Informasi Dokumen
- **Nama Proyek:** Shop Warehouse Management API
- **Peran Penyusun:** System Analyst & Software Architect
- **Versi Dokumen:** 1.0.0
- **Tanggal:** 30 September 2026
- **Status:** Spesifikasi Arsitektur Disetujui

---

### 1. Arsitektur Teknis Sistem (System Architecture)

Sistem dibangun menggunakan pendekatan **Layered Architecture** (arsitektur berlapis) dengan prinsip *Separation of Concerns* (SoC) dan *Clean Architecture* berbasis ekosistem **Java 17+** dan **Spring Boot 3.x**.

```
+-----------------------------------------------------------------+
|                       Presentation Layer                        |
|  - RestControllers (@RestController)                            |
|  - Request / Response DTOs                                      |
|  - Bean Validation (@Valid, @NotNull, etc.)                     |
|  - GlobalExceptionHandler (@RestControllerAdvice)               |
+--------------------------------+--------------------------------+
                                 | DTO
                                 v
+-----------------------------------------------------------------+
|                          Service Layer                          |
|  - ItemService, VariantService, StockService                    |
|  - Business Rules & Effective Pricing Logic                     |
|  - Transaction Management (@Transactional)                      |
|  - Out-of-Stock Guard & Concurrency Control Engine              |
+--------------------------------+--------------------------------+
                                 | Domain Entity
                                 v
+-----------------------------------------------------------------+
|                        Persistence Layer                        |
|  - Spring Data JPA Repositories (JpaRepository)                 |
|  - Custom Atomic Queries (@Modifying, conditional UPDATE)       |
|  - Hibernate ORM Entities                                       |
+--------------------------------+--------------------------------+
                                 | JDBC / SQL
                                 v
+-----------------------------------------------------------------+
|                         Database Layer                          |
|  - RDBMS (H2 In-Memory / PostgreSQL)                            |
|  - Foreign Key Constraints & Check Constraints (quantity >= 0)  |
+-----------------------------------------------------------------+
```

---

### 2. Tumpukan Teknologi (Technology Stack)

| Komponen | Pilihan Teknologi | Versi | Alasan Pemilihan |
| :--- | :--- | :--- | :--- |
| **Bahasa Pemrograman** | Java | 17 LTS atau 21 LTS | Dukungan *Records*, *Pattern Matching*, dan kinerja JVM modern. |
| **Framework Utama** | Spring Boot | 3.2.x / 3.3.x | Standar industri enterprise, ekosistem kaya, integrasi JPA mulus. |
| **ORM & Data Access** | Spring Data JPA (Hibernate 6) | 3.x | Abstraksi repositori yang kuat, mendukung *dirty checking* dan *locking*. |
| **Validasi Data** | Jakarta Bean Validation (Hibernate Validator) | 3.x | Validasi deklaratif pada level DTO sebelum mencapai *service layer*. |
| **Basis Data** | H2 Database (Dev/Test) / PostgreSQL (Prod) | 2.x | Mudah dijalankan mandiri (*zero-setup* in-memory) serta siap migrasi ke PostgreSQL. |
| **Utility & Boilerplate** | Project Lombok | 1.18.x | Mengurangi kode boilerplate (Getter, Setter, Builder, AllArgsConstructor). |
| **Dokumentasi API** | Springdoc OpenAPI (Swagger UI) | 2.5.x | Dokumentasi interaktif otomatis pada rute `/swagger-ui.html`. |
| **Build Tool** | Apache Maven / Gradle | Maven 3.9+ | Pengelolaan dependensi standar yang andal. |

---

### 3. Diagram Relasi Entitas (ERD) & Skema Database

#### 3.1 Diagram ERD Relasional
```
+-------------------------+          +-----------------------------+
|          items          | 1      * |        item_variants        |
+-------------------------+----------+-----------------------------+
| PK id          BIGINT   |          | PK id             BIGINT    |
|    code        VARCHAR  |          | FK item_id        BIGINT    |
|    name        VARCHAR  |          |    sku            VARCHAR   |
|    description TEXT     |          |    variant_name   VARCHAR   |
|    category    VARCHAR  |          |    attributes_json TEXT     |
|    base_price  DECIMAL  |          |    price          DECIMAL   |
|    created_at  TIMESTAMP|          |    is_active      BOOLEAN   |
|    updated_at  TIMESTAMP|          |    created_at     TIMESTAMP |
+-------------------------+          +--------------+--------------+
                                                    | 1
                                                    |
                                                    | 1
                                     +--------------+--------------+
                                     |           stocks            |
                                     +-----------------------------+
                                     | PK id             BIGINT    |
                                     | FK variant_id     BIGINT(UQ)|
                                     |    quantity       INTEGER   |
                                     |    version        BIGINT    |
                                     |    updated_at     TIMESTAMP |
                                     +--------------+--------------+
                                                    | 1
                                                    |
                                                    | *
                                     +--------------+--------------+
                                     |      stock_transactions     |
                                     +-----------------------------+
                                     | PK id             BIGINT    |
                                     | FK variant_id     BIGINT    |
                                     |    trx_type       VARCHAR   |
                                     |    quantity_change INTEGER  |
                                     |    balance_after   INTEGER  |
                                     |    reference_no   VARCHAR   |
                                     |    notes          TEXT      |
                                     |    created_at     TIMESTAMP |
                                     +-----------------------------+
```

#### 3.2 DDL SQL (Database Schema Definition)

```sql
-- 1. Tabel Item Induk
CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    base_price DECIMAL(15, 2) NOT NULL CHECK (base_price >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabel Varian Barang
CREATE TABLE item_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(150) NOT NULL,
    attributes_json TEXT,
    price DECIMAL(15, 2) CHECK (price IS NULL OR price >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_variant_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 3. Tabel Saldo Stok (Dengan Proteksi Kolom Non-Negatif & Optimistic Version)
CREATE TABLE stocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_variant FOREIGN KEY (variant_id) REFERENCES item_variants(id) ON DELETE CASCADE
);

-- 4. Tabel Buku Besar Mutasi Stok (Audit Ledger)
CREATE TABLE stock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    trx_type VARCHAR(30) NOT NULL, -- 'INITIAL', 'RESTOCK', 'SALE_DEDUCT', 'ADJUSTMENT'
    quantity_change INT NOT NULL,
    balance_after INT NOT NULL,
    reference_no VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_variant FOREIGN KEY (variant_id) REFERENCES item_variants(id)
);

CREATE INDEX idx_variant_sku ON item_variants(sku);
CREATE INDEX idx_stock_variant ON stocks(variant_id);
CREATE INDEX idx_tx_variant_created ON stock_transactions(variant_id, created_at);
```

---

### 4. Strategi Pencegahan Overselling & Kontrol Konkurensi

Salah satu tantangan kritis dalam sistem gudang adalah **Race Condition**: ketika dua pesanan mencoba memotong stok varian yang sama pada milidetik yang identik.

Untuk memberikan jaminan 100% bebas *overselling*, diterapkan **pendekatan pertahanan berlapis (Defense-in-Depth)**:

#### Lapisan 1: Pengecekan Kondisional Atomik di Level Database (Primary Defense)
Pengurangan stok dilakukan langsung via operasi atomik SQL yang memverifikasi kecukupan kuantitas pada saat baris dikunci:
```java
@Modifying
@Query("UPDATE Stock s SET s.quantity = s.quantity - :qty, s.updatedAt = :now " +
       "WHERE s.variant.id = :variantId AND s.quantity >= :qty")
int deductStockAtomic(@Param("variantId") Long variantId, 
                      @Param("qty") Integer qty, 
                      @Param("now") LocalDateTime now);
```
- Jika kueri mengembalikan nilai `1`: Pengurangan berhasil dieksekusi secara atomik.
- Jika kueri mengembalikan nilai `0`: Berarti stok aktual saat eksekusi **kurang dari yang diminta** (atau data tidak ada). Sistem langsung melempar `InsufficientStockException`.

#### Lapisan 2: Constraint Check pada Skema Basis Data (Database Invariant)
Kolom `quantity` memiliki constraint `CHECK (quantity >= 0)`. Jika ada bug logika yang meloloskan angka negatif, basis data akan membatalkan (*abort transaction*) secara fisik dengan pelanggaran batasan integritas.

#### Lapisan 3: Optimistic Locking dengan `@Version` (Secondary Defense)
Entitas `Stock` dilengkapi dengan kolom `@Version private Long version;`. Setiap pembaruan melalui entity lifecycle akan memvalidasi nomor versi, mencegah *lost update*.

---

### 5. Struktur Paket Proyek (Project Package Structure)

```
src/main/java/com/shop/warehouse/
│
├── ShopWarehouseApplication.java          # Main Spring Boot Runner
│
├── domain/                                # Domain Entities (JPA)
│   ├── Item.java
│   ├── ItemVariant.java
│   ├── Stock.java
│   └── StockTransaction.java
│
├── dto/                                   # Data Transfer Objects
│   ├── request/
│   │   ├── CreateItemRequest.java
│   │   ├── UpdateItemRequest.java
│   │   ├── CreateVariantRequest.java
│   │   ├── StockInRequest.java
│   │   ├── StockDeductRequest.java
│   │   └── StockAdjustRequest.java
│   └── response/
│       ├── ItemResponse.java
│       ├── VariantResponse.java
│       ├── StockResponse.java
│       ├── StockMutationResponse.java
│       └── ErrorResponse.java
│
├── repository/                            # Spring Data JPA Repositories
│   ├── ItemRepository.java
│   ├── ItemVariantRepository.java
│   ├── StockRepository.java
│   └── StockTransactionRepository.java
│
├── service/                               # Business Logic Services & Interfaces
│   ├── ItemService.java
│   ├── VariantService.java
│   ├── StockService.java
│   └── impl/
│       ├── ItemServiceImpl.java
│       ├── VariantServiceImpl.java
│       └── StockServiceImpl.java
│
├── controller/                            # REST Endpoints
│   ├── ItemController.java
│   ├── VariantController.java
│   └── StockController.java
│
├── exception/                             # Custom Exceptions & Handler
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── InsufficientStockException.java
│   ├── InvalidStockOperationException.java
│   └── GlobalExceptionHandler.java
│
└── config/                                # Configuration & OpenAPI
    └── OpenApiConfig.java
```

---

### 6. Desain Penanganan Pengecualian (Global Exception Handling)

Kelas `GlobalExceptionHandler` (`@RestControllerAdvice`) menangani seluruh pengecualian dan mengubahnya menjadi format standar HTTP:

| Pengecualian | Status HTTP | Deskripsi Kasus |
| :--- | :--- | :--- |
| `ResourceNotFoundException` | 404 NOT_FOUND | ID barang atau varian tidak ditemukan. |
| `DuplicateResourceException` | 400 BAD_REQUEST | Kode barang atau SKU varian sudah ada di sistem. |
| `InsufficientStockException` | 409 CONFLICT | Permintaan pengurangan stok melebihi stok yang tersedia. |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Kegagalan validasi Bean Validation (misal: harga negatif, field kosong). |
| `Exception` (Umum) | 500 INTERNAL_SERVER_ERROR | Terjadi kesalahan sistem internal yang tidak tertangani. |

---

### 7. Strategi Pengujian (Testing Strategy)

1. **Unit Testing:**
   - Menguji logika kalkulasi harga efektif (`VariantServiceTest`).
   - Menguji isolasi logika bisnis stok menggunakan Mockito (`StockServiceTest`).
2. **Integration Testing:**
   - Menguji alur lengkap dari HTTP request hingga basis data menggunakan `@SpringBootTest` dan `MockMvc`.
   - Menguji batasan integritas basis data.
3. **Concurrency Testing (Uji Beban Anti-Overselling):**
   - Menjalankan 50 thread simultan menggunakan `ExecutorService` untuk mencoba membeli stok yang hanya bersaldo 10 unit.
   - Verifikasi bahwa tepat 10 unit berhasil terjual, 40 thread lainnya menerima `InsufficientStockException`, dan sisa saldo di basis data tepat bernilai `0`.
