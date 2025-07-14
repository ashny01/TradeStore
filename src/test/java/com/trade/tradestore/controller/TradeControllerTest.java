package com.trade.tradestore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.tradestore.dto.TradeRequest;
import com.trade.tradestore.dto.TradeResponse;
import com.trade.tradestore.kafka.TradeProducer;
import com.trade.tradestore.service.TradeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @MockitoBean
    private TradeProducer tradeProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPostMultipleTradesToKafkaPublish() throws Exception {
        List<TradeRequest> trades = java.util.Arrays.asList(
            new TradeRequest("T1", 1, "CP-1", "B1", LocalDate.of(2020, 5, 20)),
            new TradeRequest("T2", 2, "CP-2", "B1", LocalDate.of(2021, 5, 20)),
            new TradeRequest("T2", 1, "CP-1", "B1", LocalDate.of(2021, 5, 20)),
            new TradeRequest("T3", 3, "CP-3", "B2", LocalDate.of(2014, 5, 20))
        );
        
        // Output for verification
        System.out.println("Posting trades to /api/trades/publish:");
        for (TradeRequest trade : trades) {
            System.out.println(trade);
        }

        for (TradeRequest request : trades) {
            mockMvc.perform(post("/api/trades/publish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(tradeProducer).sendTrade(request);
        }
    }

    @Test
    void shouldPostMultipleTradesAndSimulateExpiration() throws Exception {
        List<TradeRequest> tradeRequests = java.util.Arrays.asList(
                new TradeRequest("T1", 1, "CP-1", "B1", LocalDate.of(2020, 5, 20)),
                new TradeRequest("T2", 2, "CP-2", "B1", LocalDate.of(2021, 5, 20)),
                new TradeRequest("T2", 1, "CP-1", "B1", LocalDate.of(2021, 5, 20)),
                new TradeRequest("T3", 3, "CP-3", "B2", LocalDate.of(2014, 5, 20))
        );

        for (TradeRequest request : tradeRequests) {
            String expired = request.getMaturityDate().isBefore(LocalDate.now()) ? "Y" : "N";

            TradeResponse mockResponse = TradeResponse.builder()
                    .tradeId(request.getTradeId())
                    .version(request.getVersion())
                    .counterPartyId(request.getCounterPartyId())
                    .bookId(request.getBookId())
                    .maturityDate(request.getMaturityDate())
                    .createdDate(LocalDate.now())
                    .expired(expired)
                    .build();

            when(tradeService.saveTrade(request)).thenReturn(mockResponse);

            mockMvc.perform(post("/api/trades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(tradeService).saveTrade(request);
        }
    }
    @Test
    void shouldReturnBadRequestForInvalidTradeRequest() throws Exception {
        // Create JSON with invalid data that will fail validation
        String invalidJson = "{\"tradeId\":\"\",\"version\":-1,\"counterPartyId\":\"\",\"bookId\":\"\",\"maturityDate\":\"2025-07-15\"}";

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLowerVersionTrade_afterSavingHigherVersion() throws Exception {
        // First, simulate saving a trade with version 2
        TradeRequest v2Request = new TradeRequest("T2", 2, "CP-1", "B1", LocalDate.of(2021, 5, 20));

        TradeResponse v2Response = TradeResponse.builder()
            .tradeId("T2")
            .version(2)
            .counterPartyId("CP-1")
            .bookId("B1")
            .maturityDate(v2Request.getMaturityDate())
            .createdDate(LocalDate.now())
            .expired("N")
            .build();

        // Log the response for verification
        System.out.println("Saved higher version trade: " + v2Response);
        assertThat(v2Response.getVersion()).isEqualTo(2);
        assertThat(v2Response.getExpired()).isEqualTo("N");

        when(tradeService.saveTrade(v2Request)).thenReturn(v2Response);

        // Perform valid save of version 2
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2Request)))
                .andExpect(status().isOk());

        // Now send version 1 which should be rejected
        TradeRequest v1Request = new TradeRequest("T2", 1, "CP-1", "B1", LocalDate.of(2021, 5, 20));

        when(tradeService.saveTrade(v1Request))
                .thenThrow(new IllegalArgumentException("Trade rejected: Lower version received."));
        // Log rejection using System.out
        System.out.println("Attempted to save lower version trade: " + v1Request);
        // Test rejection of lower version
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v1Request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Trade rejected: Lower version received."));
    }

    @Test
    void shouldRejectPastMaturityDateTrade() throws Exception {
        TradeRequest request = new TradeRequest("T3", 1, "CP-2", "B3", LocalDate.now().minusDays(1));

        when(tradeService.saveTrade(request))
                .thenThrow(new IllegalArgumentException("Trade rejected: Maturity date is in the past."));

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void shouldPublishTradeToKafka() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setTradeId("T1");
        request.setVersion(1);
        request.setCounterPartyId("CP-1");
        request.setBookId("B1");
        request.setMaturityDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/trades/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<TradeRequest> captor = ArgumentCaptor.forClass(TradeRequest.class);
        verify(tradeProducer).sendTrade(captor.capture());

        assertThat(captor.getValue().getTradeId()).isEqualTo("T1");
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
    }

    @Test
    void shouldSaveTradeDirectly() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setTradeId("T2");
        request.setVersion(2);
        request.setCounterPartyId("CP-2");
        request.setBookId("B2");
        request.setMaturityDate(LocalDate.now().plusDays(5));

        TradeResponse response = TradeResponse.builder()
                .tradeId("T2")
                .version(2)
                .counterPartyId("CP-2")
                .bookId("B2")
                .maturityDate(request.getMaturityDate())
                .createdDate(LocalDate.now())
                .expired("N")
                .build();

        when(tradeService.saveTrade(request)).thenReturn(response);

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(tradeService).saveTrade(request);
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String malformedJson = "{ \"tradeId\": \"T5\", \"version\": \"abc\" }";

        mockMvc.perform(post("/api/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
     }
}
