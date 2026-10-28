package com.invoiceautomation.enums;

import java.math.BigDecimal;

public enum TaxCode {
    TX00("0.00"), TX05("0.05"), TX12("0.12"), TX18("0.18"), TX28("0.28");

    private final BigDecimal rate;

    TaxCode(String rate) {
        this.rate = new BigDecimal(rate);
    }

    public BigDecimal rate() {
        return rate;
    }
}