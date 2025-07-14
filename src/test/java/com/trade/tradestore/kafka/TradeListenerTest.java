package com.trade.tradestore.kafka;

import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.kafka.TradeListener;
import com.trade.tradestore.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;


class TradeListenerTest {

    @Mock
    private TradeService tradeService;

    @InjectMocks
    private TradeListener tradeListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMultipleIncomingTrades() {
        /*List<TradeRequest> trades = Arrays.asList(
                new TradeRequest("T1", 1, "CP-1", "B1", LocalDate.of(2020, 5, 20)),
                new TradeRequest("T2", 2, "CP-2", "B1", LocalDate.of(2021, 5, 20)),
                new TradeRequest("T2", 1, "CP-1", "B1", LocalDate.of(2021, 5, 20)),
                new TradeRequest("T3", 3, "CP-3", "B2", LocalDate.of(2014, 5, 20))
        );*/
        List<TradeRequest> trades = IntStream.range(1, 101)
                .mapToObj(i -> new TradeRequest("T" + i, 1, "CP-" + i, "B" + (i % 3),
                        LocalDate.now().plusDays(i)))
                .collect(Collectors.toList());

        for (TradeRequest trade : trades) {
            tradeListener.consume(trade);
        }

        // Verify all trades attempted to be saved
        verify(tradeService, times(trades.size())).saveTrade(any());
    }
}