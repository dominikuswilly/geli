package com.shop.warehouse.service.impl;

import com.shop.warehouse.domain.ItemVariant;
import com.shop.warehouse.domain.Stock;
import com.shop.warehouse.dto.response.DashboardSummaryResponse;
import com.shop.warehouse.repository.ItemRepository;
import com.shop.warehouse.repository.ItemVariantRepository;
import com.shop.warehouse.repository.StockRepository;
import com.shop.warehouse.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ItemRepository itemRepository;
    private final ItemVariantRepository variantRepository;
    private final StockRepository stockRepository;

    public DashboardServiceImpl(ItemRepository itemRepository,
                                ItemVariantRepository variantRepository,
                                StockRepository stockRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        DashboardSummaryResponse summary = new DashboardSummaryResponse();

        long totalItems = itemRepository.count();
        List<ItemVariant> variants = variantRepository.findAll();
        long totalVariants = variants.size();

        long totalStock = 0;
        BigDecimal totalValuation = BigDecimal.ZERO;
        long outOfStock = 0;
        long lowStock = 0;
        long inStock = 0;

        for (ItemVariant v : variants) {
            int qty = 0;
            Stock s = stockRepository.findByVariantId(v.getId()).orElse(null);
            if (s != null) {
                qty = s.getQuantity();
            }

            totalStock += qty;
            BigDecimal price = v.getEffectivePrice();
            totalValuation = totalValuation.add(price.multiply(BigDecimal.valueOf(qty)));

            if (qty <= 0) {
                outOfStock++;
            } else if (qty <= 5) {
                lowStock++;
                inStock++;
            } else {
                inStock++;
            }
        }

        summary.setTotalItems(totalItems);
        summary.setTotalVariants(totalVariants);
        summary.setTotalStockQuantity(totalStock);
        summary.setTotalValuation(totalValuation);
        summary.setOutOfStockCount(outOfStock);
        summary.setLowStockCount(lowStock);
        summary.setInStockCount(inStock);

        return summary;
    }
}
