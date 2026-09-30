# Business Requirements Document (BRD)
## Sistem Manajemen Gudang Toko (Shop Warehouse Management System)

---

### Informasi Dokumen
- **Nama Proyek:** Shop Warehouse Management System (RESTful API Backend)
- **Peran Penyusun:** System Analyst
- **Versi Dokumen:** 1.0.0
- **Tanggal:** 30 September 2026
- **Zona Waktu:** Asia/Jakarta (UTC+7)
- **Status:** Disetujui untuk Pengembangan Teknis

---

### 1. Ringkasan Eksekutif (Executive Summary)

Perkembangan operasional toko ritel modern menuntut pengelolaan stok barang (inventory) yang cepat, akurat, dan terintegrasi secara *real-time*. Seringkali terjadi kendala operasional seperti selisih stok fisik dengan data sistem, penjualan barang yang persediaannya telah habis (*overselling*), serta kesulitan melacak produk yang memiliki variasi atribut (seperti ukuran, warna, atau model) dengan skema harga yang berbeda.

Dokumen Kebutuhan Bisnis (*Business Requirements Document* / BRD) ini mendefinisikan kebutuhan bisnis untuk pengembangan sistem *backend* berbasis **RESTful API** menggunakan **Spring Boot 3.x** dan **Java 17+**. Sistem ini bertujuan menjadi pusat data persediaan barang toko yang mencatat item, varian, harga, serta mutasi stok dengan garansi pencegahan *overselling*.

---

### 2. Tujuan & Sasaran Bisnis (Business Goals & Objectives)

#### 2.1 Tujuan Bisnis
1. **Otomatisasi & Sentralisasi Inventaris:** Menyediakan satu sumber kebenaran (*single source of truth*) data inventaris toko untuk item dan seluruh variannya.
2. **Eliminasi Kasus *Overselling*:** Mencegah terjadinya penjualan barang yang stoknya nol atau kurang dari kuantitas pemesanan secara konsisten dan teruji.
3. **Fleksibilitas Manajemen Varian & Harga:** Memfasilitasi katalog produk yang dinamis di mana suatu produk induk dapat memiliki beragam varian dengan harga pokok maupun harga jual yang spesifik.
4. **Efisiensi Operasional:** Mempercepat proses pencatatan stok masuk (*stock-in*), stok keluar (*stock-out*), dan penyesuaian stok (*stock adjustment*).

#### 2.2 Indikator Keberhasilan (Success Metrics / KPI)
- **Tingkat Kejadian Overselling:** 0% insiden penjualan barang habis.
- **Akurasi Pencatatan Stok:** 100% konsistensi antara mutasi transaksi stok dengan saldo akhir persediaan.
- **Waktu Respon Pengecekan Stok:** < 100 milidetik untuk operasi kueri ketersediaan stok.
- **Ketersediaan Layanan API:** 99.9% *uptime* untuk integrasi sistem kasir (POS) maupun e-commerce.

---

### 3. Pemangku Kepentingan (Stakeholders Analysis)

| Peran Pemangku Kepentingan | Deskripsi Peran | Kepentingan / Kebutuhan Utama |
| :--- | :--- | :--- |
| **Manajer Operasional / Toko** | Pengambil keputusan tingkat toko | Memantau ringkasan ketersediaan barang, performa perputaran stok, dan validitas valuasi stok. |
| **Staf / Admin Gudang** | Pelaksana operasional gudang | Menambah produk baru, mengelola varian barang, mencatat stok masuk (restock), dan opname stok. |
| **Sistem Penjualan (POS / E-Commerce)** | Sistem klien eksternal pengonsumsi API | Melakukan kueri stok secara cepat, validasi harga, dan pemotongan stok otomatis saat transaksi penjualan. |
| **Tim Pengembang (Software Engineer)** | Pelaksana teknis sistem | Memiliki spesifikasi kebutuhan bisnis yang jelas, terstruktur, dan tidak ambigu untuk diimplementasikan. |

---

### 4. Permasalahan Bisnis Saat Ini (Current Business Pain Points)

1. **Pencatatan Varian Produk yang Terpisah:**
   Produk dengan ukuran atau warna berbeda sering kali didaftarkan sebagai item terpisah tanpa relasi hierarki, menyulitkan laporan konsolidasi per produk.
2. **Ketiadaan Mekanisme Penahan Stok Habis (*Out of Stock Prevention*):**
   Pada saat terjadi lonjakan pembelian, pesanan tetap diterima meskipun stok fisik di gudang telah kosong, memicu komplain pelanggan dan pembatalan pesanan.
3. **Ketidaksesuaian Harga Varian:**
   Kebutuhan menetapkan harga berbeda untuk varian tertentu (misal: ukuran XL lebih mahal dari ukuran S) sulit dikelola jika sistem hanya mendukung satu harga per produk induk.
4. **Audit Mutasi Stok Lemah:**
   Tidak adanya jejak riwayat alasan penambahan atau pengurangan stok menyulitkan investigasi saat terjadi perbedaan stok saat audit fisik (*stock opname*).

---

### 5. Ruang Lingkup Bisnis (Business Scope)

#### 5.1 Dalam Lingkup (In-Scope)
- Manajemen Master Data Item (Produk Induk): pembuatan, pembacaan, pembaruan, dan penonaktifan/penghapusan data produk.
- Manajemen Master Data Varian: pembuatan dan pengelolaan atribut varian (ukuran, warna, SKU khusus) yang terasosiasi ke item induk.
- Manajemen Harga: konfigurasi harga dasar pada level item dan *override* harga khusus pada level varian.
- Manajemen Saldo & Mutasi Stok: pencatatan kuantitas stok per varian, penambahan stok (*restock*), dan penyesuaian stok (*adjustment*).
- Validasi & Proteksi Stok: penolakan otomatis terhadap transaksi pengurangan stok apabila kuantitas yang diminta melebihi stok yang tersedia.
- Penyediaan Antarmuka RESTful API berbasis JSON dengan persistensi data relasional.

#### 5.2 Di Luar Lingkup (Out-of-Scope)
- Antarmuka visual berbasis web/mobile frontend (fokus murni pada arsitektur Backend REST API).
- Integrasi modul pembayaran (*payment gateway*) dan jasa pengiriman logistik pihak ketiga.
- Multi-warehouse/multi-lokasi gudang geografis (pada fase awal difokuskan untuk sistem gudang tunggal/sentral toko).

---

### 6. Kebutuhan Bisnis Rinci (Detailed Business Requirements)

#### BR-01: Manajemen Item (Barang Induk)
- **BR-01.1:** Sistem harus mampu mencatat data barang induk yang mencakup kode barang/SKU induk, nama barang, deskripsi, kategori, dan harga dasar (*base price*).
- **BR-01.2:** Setiap barang induk harus memiliki pengenal unik (*identifier*) yang tidak boleh duplikat.
- **BR-01.3:** Sistem harus mengizinkan pembaruan informasi barang induk dan penonaktifan jika barang tidak lagi dipasarkan.

#### BR-02: Manajemen Varian Produk
- **BR-02.1:** Setiap barang induk dapat memiliki 0 hingga banyak varian (misal: Warna: Merah/Biru, Ukuran: S/M/L).
- **BR-02.2:** Setiap varian wajib memiliki Stock Keeping Unit (SKU) unik tingkat varian untuk memudahkan *barcode scanning* di gudang.
- **BR-02.3:** Jika barang tidak memiliki varian khusus, sistem harus secara otomatis atau default menganggap barang tersebut memiliki varian tunggal (*default variant*).

#### BR-03: Penetapan Harga (Pricing)
- **BR-03.1:** Sistem harus mendukung penetapan harga dasar (*base price*) pada tingkat barang induk.
- **BR-03.2:** Sistem harus mengizinkan varian untuk memiliki harga tersendiri (*specific variant price*). Jika harga varian tidak ditentukan secara khusus, maka harga varian akan mengacu pada harga dasar barang induk.
- **BR-03.3:** Nilai harga harus bertipe non-negatif dan presisi mata uang rupiah/desimal standar.

#### BR-04: Pelacakan Inventaris & Kontrol Stok
- **BR-04.1:** Kuantitas stok persediaan wajib dilacak secara spesifik pada level varian (atau item jika tanpa varian).
- **BR-04.2:** Sistem harus mendukung pencatatan penambahan stok (*stock in / restock*) dengan mencatat kuantitas dan catatan referensi.
- **BR-04.3:** Sistem harus mendukung penyesuaian stok (*stock adjustment*) untuk kebutuhan rekonsiliasi saat *stock opname* fisik.
- **BR-04.4:** Sistem harus mampu mengembalikan status ketersediaan barang secara seketika (*real-time status*: Tersedia / Habis / Stok Rendah).

#### BR-05: Pencegahan Penjualan Barang Habis (Anti-Overselling Guard)
- **BR-05.1:** Setiap permintaan pengurangan stok untuk keperluan penjualan/order wajib divalidasi terhadap saldo stok terkini.
- **BR-05.2:** Apabila kuantitas yang diminta > kuantitas stok yang tersedia, sistem **WAJIB** menolak transaksi secara atomik dan mengembalikan respon kegagalan yang informatif.
- **BR-05.3:** Pengurangan stok tidak boleh mengakibatkan saldo stok bernilai negatif (< 0) dalam kondisi konkurensi apa pun.

---

### 7. Asumsi dan Batasan Bisnis (Assumptions and Constraints)

#### 7.1 Asumsi Bisnis
1. Seluruh transaksi inventaris dicatat dalam satuan bilangan bulat (*integer units*, misal: pcs, unit).
2. Mata uang operasional yang digunakan adalah Rupiah (IDR).
3. Pengurangan stok terjadi pada saat konfirmasi pemesanan atau *checkout* dari sistem penjualan.

#### 7.2 Batasan Teknis dan Kepatuhan
1. **Teknologi Wajib:** Berbasis Java versi 17+ dan framework Spring Boot 3.x.
2. **Kepatuhan REST:** Seluruh interaksi data dilakukan melalui protokol HTTP standar dengan format pertukaran JSON.
3. **Persistensi Data:** Seluruh data item, varian, dan stok harus tersimpan persisten dalam basis data relasional.
4. **Integritas Konkurensi:** Menjamin *ACID properties* pada setiap transaksi perubahan stok untuk mencegah *race condition*.

---

### 8. Penilaian Risiko Bisnis & Mitigasi

| Risiko Bisnis | Tingkat Dampak | Probabilitas | Rencana Mitigasi |
| :--- | :--- | :--- | :--- |
| **Race Condition saat Pembelian Bersamaan** | Tinggi | Sedang | Menerapkan mekanisme penguncian atomik (*optimistic/pessimistic locking* atau kueri bersyarat pada basis data). |
| **Duplikasi SKU Varian** | Sedang | Sedang | Menerapkan batasan *unique constraint* pada kolom SKU di tingkat basis data dan validasi pada *service layer*. |
| **Penghapusan Barang yang Masih Memiliki Riwayat Stok** | Tinggi | Rendah | Menggunakan mekanisme *soft-delete* atau pembatasan *foreign key constraint* agar riwayat audit tetap terjaga. |
