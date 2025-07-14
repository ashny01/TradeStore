package com.trade.tradestore.service;

import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.dto.TradeResponse;
import com.trade.tradestore.model.Trade;
import com.trade.tradestore.repository.TradeRepository;
import com.trade.tradestore.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSaveValidTrade_whenNoExistingTrade() {
        TradeRequest request = TradeRequest.builder()
                .tradeId("T1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(5))
                .build();

        when(tradeRepository.findTopByTradeIdOrderByVersionDesc("T1")).thenReturn(Optional.empty());

        TradeResponse response = tradeService.saveTrade(request);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(captor.capture());
        assertThat(captor.getValue().getTradeId()).isEqualTo("T1");
        assertThat(response.getExpired()).isEqualTo("N");
    }

    @Test
    void shouldRejectTradeWithPastMaturityDate() {
        TradeRequest request = TradeRequest.builder()
                .tradeId("T3")
                .version(3)
                .counterPartyId("CP-3")
                .bookId("B2")
                .maturityDate(LocalDate.now().minusDays(1))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> tradeService.saveTrade(request));
        assertThat(ex.getMessage()).contains("Maturity date is in the past");
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void shouldRejectLowerVersionTrade_whenHigherExists() {
        Trade existing = Trade.builder()
                .tradeId("T2")
                .version(2)
                .counterPartyId("CP-2")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(10))
                .expired("N")
                .build();

        when(tradeRepository.findTopByTradeIdOrderByVersionDesc("T2"))
                .thenReturn(Optional.of(existing));

        TradeRequest request = TradeRequest.builder()
                .tradeId("T2")
                .version(1)
                .counterPartyId("CP-2")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(10))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> tradeService.saveTrade(request));
        assertThat(ex.getMessage()).contains("Lower version received");
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void shouldOverrideSameOrHigherVersionTrade() {
        Trade existing = Trade.builder()
                .tradeId("T2")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(10))
                .expired("N")
                .build();

        when(tradeRepository.findTopByTradeIdOrderByVersionDesc("T2"))
                .thenReturn(Optional.of(existing));

        TradeRequest request = TradeRequest.builder()
                .tradeId("T2")
                .version(2)
                .counterPartyId("CP-2")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(15))
                .build();

        tradeService.saveTrade(request);

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void shouldExpireMaturedTrades() {
        Trade expiredTrade = Trade.builder()
                .tradeId("T3")
                .version(1)
                .counterPartyId("CP-3")
                .bookId("B2")
                .maturityDate(LocalDate.now().minusDays(1))
                .expired("N")
                .build();

        when(tradeRepository.findByMaturityDateBeforeAndExpired(any(), eq("N")))
                .thenReturn(Arrays.asList(expiredTrade));

        tradeService.expireMaturedTrades();

        verify(tradeRepository).save(argThat(trade -> trade.getTradeId().equals("T3") && "Y".equals(trade.getExpired())));
    }
}