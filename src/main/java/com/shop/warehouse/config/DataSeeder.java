package com.shop.warehouse.config;

import com.shop.warehouse.domain.Item;
import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.domain.StockTransaction;
import com.shop.warehouse.domain.TransactionType;
import com.shop.warehouse.repository.ItemRepository;
import com.shop.warehouse.repository.ItemVariantRepository;
import com.shop.warehouse.repository.StockRepository;
import com.shop.warehouse.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ItemRepository itemRepository;
    private final ItemVariantRepository variantRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository transactionRepository;

    public DataSeeder(ItemRepository itemRepository,
                      ItemVariantRepository variantRepository,
                      StockRepository stockRepository,
                      StockTransactionRepository transactionRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() > 0) {
            log.info("Database sudah memiliki data awal, melewati proses seeding.");
            return;
        }

        log.info("Memulai inisialisasi data sampel inventaris gudang...");

        // 1. Kemeja Katun Oxford Pria
        Item item1 = new Item(
                "SHIRT-OXF",
                "Kemeja Katun Oxford Pria",
                "Kemeja katun premium dengan jahitan rapi, cocok untuk gaya kasual dan formal.",
                "Pakaian Pria",
                new BigDecimal("149000.00")
        );
        item1 = itemRepository.save(item1);

        createVariantWithStock(item1, "SHIRT-OXF-WHT-M", "Putih - M", "{\"color\":\"Putih\",\"size\":\"M\"}", null, 25);
        createVariantWithStock(item1, "SHIRT-OXF-WHT-L", "Putih - L", "{\"color\":\"Putih\",\"size\":\"L\"}", null, 18);
        createVariantWithStock(item1, "SHIRT-OXF-BLU-XL", "Biru Muda - XL", "{\"color\":\"Biru Muda\",\"size\":\"XL\"}", new BigDecimal("169000.00"), 8);

        // 2. Kaos Polos Heavyweight Cotton
        Item item2 = new Item(
                "TSHIRT-HVY",
                "Kaos Polos Heavyweight Cotton 24s",
                "Kaos katun tebal 24s combed, tidak menerawang, sangat nyaman dipakai harian.",
                "Kaos & T-Shirt",
                new BigDecimal("89000.00")
        );
        item2 = itemRepository.save(item2);

        createVariantWithStock(item2, "TSHIRT-HVY-BLK-S", "Hitam - S", "{\"color\":\"Hitam\",\"size\":\"S\"}", null, 30);
        createVariantWithStock(item2, "TSHIRT-HVY-BLK-M", "Hitam - M", "{\"color\":\"Hitam\",\"size\":\"M\"}", null, 15);
        createVariantWithStock(item2, "TSHIRT-HVY-BLK-L", "Hitam - L", "{\"color\":\"Hitam\",\"size\":\"L\"}", null, 3); // Low Stock
        createVariantWithStock(item2, "TSHIRT-HVY-BLK-XXL", "Hitam - XXL", "{\"color\":\"Hitam\",\"size\":\"XXL\"}", new BigDecimal("99000.00"), 0); // Out of stock

        // 3. Celana Chino Slim Fit
        Item item3 = new Item(
                "CHINO-SLM",
                "Celana Panjang Chino Slim Fit",
                "Celana chino stretch katun twill elastis, potongan slim fit modern.",
                "Celana Pria",
                new BigDecimal("199000.00")
        );
        item3 = itemRepository.save(item3);

        createVariantWithStock(item3, "CHINO-KRM-30", "Krem - Size 30", "{\"color\":\"Krem\",\"size\":\"30\"}", null, 12);
        createVariantWithStock(item3, "CHINO-KRM-32", "Krem - Size 32", "{\"color\":\"Krem\",\"size\":\"32\"}", null, 20);
        createVariantWithStock(item3, "CHINO-GRY-34", "Abu-abu - Size 34", "{\"color\":\"Abu-abu\",\"size\":\"34\"}", new BigDecimal("219000.00"), 5);

        // 4. Sepatu Sneaker Canvas Classic
        Item item4 = new Item(
                "SNK-CANVAS",
                "Sepatu Sneaker Canvas Low Classic",
                "Sneaker kanvas vulkanisir dengan sol karet anti-slip dan insole empuk.",
                "Sepatu",
                new BigDecimal("279000.00")
        );
        item4 = itemRepository.save(item4);

        createVariantWithStock(item4, "SNK-WHT-41", "Putih Klasik - 41", "{\"color\":\"Putih\",\"size\":\"41\"}", null, 10);
        createVariantWithStock(item4, "SNK-WHT-42", "Putih Klasik - 42", "{\"color\":\"Putih\",\"size\":\"42\"}", null, 14);
        createVariantWithStock(item4, "SNK-BLK-43", "All Black - 43", "{\"color\":\"Hitam\",\"size\":\"43\"}", new BigDecimal("299000.00"), 0); // Out of stock

        log.info("Inisialisasi sampel data inventaris selesai. Total item: {}, Total varian: {}",
                itemRepository.count(), variantRepository.count());
    }

    private void createVariantWithStock(Item item, String sku, String name, String attributes, BigDecimal price, int initialStock) {
        ItemVariant variant = new ItemVariant(item, sku, name, attributes, price);
        ItemVariant savedVariant = variantRepository.save(variant);

        Stock stock = new Stock(savedVariant, initialStock);
        stockRepository.save(stock);
        savedVariant.setStock(stock);

        if (initialStock > 0) {
            StockTransaction tx = new StockTransaction(
                    savedVariant,
                    TransactionType.INITIAL,
                    initialStock,
                    initialStock,
                    "INIT-" + sku,
                    "Inisialisasi stok awal sistem"
            );
            transactionRepository.save(tx);
        }
    }
}
