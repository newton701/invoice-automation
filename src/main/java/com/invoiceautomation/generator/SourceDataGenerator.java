package com.invoiceautomation.generator;

import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.enums.GrnStatus;
import com.invoiceautomation.enums.PoStatus;
import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.enums.VendorStatus;
import com.invoiceautomation.model.GoodsReceipt;
import com.invoiceautomation.model.GoodsReceiptLine;
import com.invoiceautomation.model.PurchaseOrder;
import com.invoiceautomation.model.PurchaseOrderLine;
import com.invoiceautomation.model.Vendor;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Step 1 of generation: vendors, purchase orders, PO lines, goods receipts and receipt lines.
 * Uses one seeded Random and only ordered collections, so output is reproducible.
 */
public final class SourceDataGenerator {

    private record Product(String sku, String description, BigDecimal basePrice, TaxCode taxCode) { }

    private static final String[] NAME_FIRST = {"Apex", "Bharat", "Crest", "Delta", "Eastern", "Falcon",
            "Global", "Horizon", "Indus", "Jupiter", "Kaveri", "Lotus", "Metro", "Nova", "Orion",
            "Prime", "Quest", "Rapid", "Sterling", "Trident"};
    private static final String[] NAME_SECOND = {"Industrial", "Logistics", "Components", "Supplies",
            "Engineering", "Trading", "Materials", "Systems", "Tools", "Packaging"};
    private static final String[] NAME_SUFFIX = {"Pvt Ltd", "Ltd", "Industries", "Enterprises", "& Co", "Corp"};
    private static final int[] PAYMENT_TERMS = {15, 30, 45, 60};
    private static final String[] NOUNS = {"Steel Bolt", "Copper Wire", "Hydraulic Pump", "Office Chair",
            "Laptop Stand", "Packing Tape", "Safety Gloves", "LED Panel", "Cable Tray", "Ball Bearing",
            "Toner Cartridge", "Cleaning Solvent", "Pallet Wrap", "Air Filter", "Industrial Fan",
            "Server Rack", "Network Switch", "Paper Ream", "Welding Rod", "Lubricant Oil"};
    private static final String[] GRADES = {"Standard", "Heavy Duty", "Premium", "Compact", "Industrial"};

    private static final int MAX_LINES_PER_PO = 8;
    private static final int PCT_POS_WITHOUT_RECEIPT = 20;
    private static final int PCT_PARTIAL_POS = 25;

    private final GeneratorConfig config;
    private final IdFactory ids;
    private final Random random;

    public SourceDataGenerator(GeneratorConfig config, IdFactory ids, Random random) {
        this.config = config;
        this.ids = ids;
        this.random = random;
    }

    public SourceData generate() {
        List<Product> catalog = buildCatalog();
        List<Vendor> vendors = generateVendors();

        List<PurchaseOrder> orders = new ArrayList<>();
        List<PurchaseOrderLine> orderLines = new ArrayList<>();
        generatePurchaseOrders(vendors, catalog, orders, orderLines);

        List<GoodsReceipt> receipts = new ArrayList<>();
        List<GoodsReceiptLine> receiptLines = new ArrayList<>();
        generateReceipts(orders, orderLines, receipts, receiptLines);

        return new SourceData(vendors, orders, orderLines, receipts, receiptLines);
    }

    // ------------------------------------------------------------------ catalog

    private List<Product> buildCatalog() {
        List<Product> catalog = new ArrayList<>();
        int skuNumber = 10001;
        for (String grade : GRADES) {
            for (String noun : NOUNS) {
                // log-uniform price between 50 and 50,000 INR: many cheap items, few expensive ones
                BigDecimal base = Money.round(BigDecimal.valueOf(50 * StrictMath.pow(1000, random.nextDouble())));
                catalog.add(new Product("SKU-" + skuNumber++, grade + " " + noun, base, pickTaxCode()));
            }
        }
        return catalog;
    }

    private TaxCode pickTaxCode() {
        double r = random.nextDouble();
        return r < 0.05 ? TaxCode.TX00 : r < 0.15 ? TaxCode.TX05 : r < 0.30 ? TaxCode.TX12
                                                                   : r < 0.90 ? TaxCode.TX18 : TaxCode.TX28;
    }

    // ------------------------------------------------------------------ vendors

    private List<Vendor> generateVendors() {
        long windowDays = ChronoUnit.DAYS.between(config.windowStart(), config.windowEnd());
        Set<String> usedNames = new HashSet<>();
        Set<String> usedTaxNumbers = new HashSet<>();
        List<Vendor> vendors = new ArrayList<>();

        for (int i = 0; i < config.vendors(); i++) {
            String vendorId = ids.nextVendorId();

            double s = random.nextDouble();
            VendorStatus status = s < 0.05 ? VendorStatus.CLOSED : s < 0.10 ? VendorStatus.ON_HOLD : VendorStatus.ACTIVE;

            LocalDate onboarded;
            if (status == VendorStatus.ACTIVE && random.nextInt(10) == 0) {
                onboarded = config.windowStart().plusDays(random.nextInt(120));   // new vendor, onboarded in window
            } else {
                onboarded = LocalDate.of(2022, 1, 1).plusDays(random.nextInt(1460));
            }
            LocalDate closed = status == VendorStatus.CLOSED
                    ? config.windowStart().plusDays(120 + random.nextInt((int) windowDays - 150))
                    : null;

            String name;
            do {
                name = NAME_FIRST[random.nextInt(NAME_FIRST.length)] + " "
                        + NAME_SECOND[random.nextInt(NAME_SECOND.length)] + " "
                        + NAME_SUFFIX[random.nextInt(NAME_SUFFIX.length)];
            } while (!usedNames.add(name));

            String taxNo;
            do {
                taxNo = "99" + randomLetters(5) + String.format("%04d", random.nextInt(10000))
                        + randomLetters(1) + "1Z" + random.nextInt(10);
            } while (!usedTaxNumbers.add(taxNo));

            vendors.add(new Vendor(vendorId, IdFactory.vendorCode(vendorId), name, status,
                    random.nextInt(100) >= 2,                         // ~2% vendors do not require a PO
                    PAYMENT_TERMS[random.nextInt(PAYMENT_TERMS.length)],
                    taxNo, onboarded, closed));
        }
        return vendors;
    }

    private String randomLetters(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ purchase orders

    private void generatePurchaseOrders(List<Vendor> vendors, List<Product> catalog,
                                        List<PurchaseOrder> orders, List<PurchaseOrderLine> orderLines) {
        int extraLines = config.poLines() - config.purchaseOrders();
        if (extraLines < 0) {
            throw new IllegalStateException("poLines must be >= purchaseOrders (every PO needs a line)");
        }
        int[] lineCounts = new int[config.purchaseOrders()];
        java.util.Arrays.fill(lineCounts, 1);
        while (extraLines > 0) {
            int i = random.nextInt(lineCounts.length);
            if (lineCounts[i] < MAX_LINES_PER_PO) {
                lineCounts[i]++;
                extraLines--;
            }
        }

        LocalDate latestPoDate = config.windowEnd().minusDays(90);   // leaves room for receipts and invoices
        for (int i = 0; i < config.purchaseOrders(); i++) {
            // skewed pick: a few vendors get most of the orders
            Vendor vendor = vendors.get((int) (vendors.size() * StrictMath.pow(random.nextDouble(), 1.5)));

            LocalDate earliest = vendor.onboardedDate().isAfter(config.windowStart())
                    ? vendor.onboardedDate() : config.windowStart();
            LocalDate latest = vendor.closedDate() != null && vendor.closedDate().isBefore(latestPoDate)
                    ? vendor.closedDate() : latestPoDate;
            LocalDate poDate = earliest.plusDays(random.nextInt((int) ChronoUnit.DAYS.between(earliest, latest) + 1));

            String poId = ids.nextPoId();
            List<PurchaseOrderLine> lines = new ArrayList<>();
            Set<Integer> usedProducts = new HashSet<>();
            for (int n = 1; n <= lineCounts[i]; n++) {
                int productIndex;
                do {
                    productIndex = random.nextInt(catalog.size());
                } while (!usedProducts.add(productIndex));
                Product p = catalog.get(productIndex);

                BigDecimal price = Money.round(p.basePrice().multiply(BigDecimal.valueOf(0.95 + random.nextDouble() * 0.10)));
                int qty = 10 + (int) (490 * random.nextDouble() * random.nextDouble());
                lines.add(PurchaseOrderLine.of(poId, n, p.sku(), p.description(), qty, price, p.taxCode()));
            }
            orders.add(PurchaseOrder.of(poId, vendor.vendorId(), poDate,
                    poDate.plusDays(7 + random.nextInt(24)), PoStatus.OPEN, lines));
            orderLines.addAll(lines);
        }
    }

    // ------------------------------------------------------------------ goods receipts

    private void generateReceipts(List<PurchaseOrder> orders, List<PurchaseOrderLine> orderLines,
                                  List<GoodsReceipt> receipts, List<GoodsReceiptLine> receiptLines) {
        int poCount = orders.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < poCount; i++) {
            order.add(i);
        }
        Collections.shuffle(order, random);

        int receivingCount = poCount - poCount * PCT_POS_WITHOUT_RECEIPT / 100;
        int extraReceipts = config.receipts() - receivingCount;
        if (extraReceipts < 0) {
            throw new IllegalStateException("receipts must be >= number of POs that receive goods");
        }

        int[] receiptCount = new int[poCount];
        boolean[] partial = new boolean[poCount];
        List<Integer> receiving = order.subList(0, receivingCount);
        for (int idx : receiving) {
            receiptCount[idx] = 1;
            partial[idx] = random.nextInt(100) < PCT_PARTIAL_POS;
        }
        while (extraReceipts > 0) {
            int idx = receiving.get(random.nextInt(receivingCount));
            int cap = partial[idx] ? 2 : 3;
            if (receiptCount[idx] < cap) {
                receiptCount[idx]++;
                extraReceipts--;
            }
        }

        for (int i = 0; i < poCount; i++) {
            PurchaseOrder po = orders.get(i);
            if (receiptCount[i] == 0) {
                if (random.nextInt(100) < 3) {
                    orders.set(i, po.withStatus(PoStatus.CANCELLED));
                }
                continue;
            }
            List<PurchaseOrderLine> lines = new ArrayList<>();
            for (PurchaseOrderLine l : orderLines) {
                if (l.poId().equals(po.poId())) {
                    lines.add(l);
                }
            }
            receiveGoods(po, lines, receiptCount[i], partial[i], receipts, receiptLines);
            orders.set(i, po.withStatus(partial[i] ? PoStatus.PARTIALLY_RECEIVED : PoStatus.FULLY_RECEIVED));
        }
    }

    private void receiveGoods(PurchaseOrder po, List<PurchaseOrderLine> lines, int count, boolean partial,
                              List<GoodsReceipt> receipts, List<GoodsReceiptLine> receiptLines) {
        int[] received = new int[lines.size()];
        LocalDate date = po.poDate().plusDays(3 + random.nextInt(25));

        for (int r = 1; r <= count; r++) {
            boolean last = r == count;
            String grnId = ids.nextGrnId();
            List<GoodsReceiptLine> drafted = new ArrayList<>();

            for (int j = 0; j < lines.size(); j++) {
                PurchaseOrderLine line = lines.get(j);
                int remaining = line.orderedQty() - received[j];
                int qty;
                if (!last || partial) {
                    qty = chunk(remaining);                       // never completes the line
                } else {
                    qty = remaining;                              // completes the line ...
                    if (random.nextInt(100) < 3) {                // ... occasionally over-delivers
                        qty += 1 + random.nextInt(Math.max(1, line.orderedQty() / 20));
                    }
                }
                if (qty == 0) {
                    continue;
                }
                received[j] += qty;
                drafted.add(new GoodsReceiptLine(IdFactory.grnLineId(grnId, drafted.size() + 1), grnId,
                        line.poLineId(), qty, GoodsReceiptLine.classify(received[j], line.orderedQty())));
            }
            if (drafted.isEmpty()) {
                throw new IllegalStateException("Receipt " + grnId + " has no lines");
            }
            receipts.add(new GoodsReceipt(grnId, po.poId(), date, GrnStatus.POSTED));
            receiptLines.addAll(drafted);
            date = date.plusDays(3 + random.nextInt(12));
        }
    }

    /** 30-70% of what is still open, at least 1 and always leaving at least 1 open. */
    private int chunk(int remaining) {
        if (remaining < 2) {
            return 0;
        }
        int qty = (int) (remaining * (0.3 + random.nextDouble() * 0.4));
        return Math.max(1, Math.min(remaining - 1, qty));
    }
}