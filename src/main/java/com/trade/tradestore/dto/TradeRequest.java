package com.trade.tradestore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {
    @NotBlank
    private String tradeId;

    @Min(1)
    private int version;

    @NotBlank
    private String counterPartyId;

    @NotBlank
    private String bookId;

    private LocalDate maturityDate;
}
