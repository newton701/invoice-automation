
package com.invoiceautomation.model;

import com.invoiceautomation.enums.MatchStatus;

import java.math.BigDecimal;

public record MatchResult(
        String matchId,
        String invoiceLineId,
        String poLineId,
        String grnLineId,
        BigDecimal priceVariancePct,
        int qtyVariance,
        BigDecimal taxVariance,
        MatchStatus lineMatchStatus
) {
}
