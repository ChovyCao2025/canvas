package org.chovy.canvas.domain.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.TagImportSourceDO;
import org.chovy.canvas.dal.mapper.TagImportSourceMapper;
import org.chovy.canvas.dto.TagImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TagImportSourceServiceReactiveTest {

    @Test
    void runAsyncDefersSourceLookupRemoteFetchAndImportUntilSubscribed() {
        TagImportSourceMapper mapper = mock(TagImportSourceMapper.class);
        TagImportService importService = mock(TagImportService.class);
        AtomicInteger remoteCalls = new AtomicInteger();
        TagImportResult result = successfulResult();

        when(mapper.selectById(7L)).thenReturn(enabledSource());
        when(importService.importRows(eq("API_PULL"), eq(null), eq("http://93.184.216.34/tags"), any()))
                .thenReturn(result);
        TagImportSourceService service = new TagImportSourceService(
                mapper, importService, new ObjectMapper(), webClient(remoteCalls));

        Mono<TagImportResult> run = service.runAsync(7L);

        verifyNoInteractions(mapper, importService);
        assertThat(remoteCalls).hasValue(0);

        StepVerifier.create(run)
                .expectNext(result)
                .verifyComplete();

        verify(mapper).selectById(7L);
        verify(importService).importRows(eq("API_PULL"), eq(null), eq("http://93.184.216.34/tags"), any());
        assertThat(remoteCalls).hasValue(1);
    }

    @Test
    void runAsyncSchedulesBlockingLookupAndImportAwayFromSubscribingThread() {
        TagImportSourceMapper mapper = mock(TagImportSourceMapper.class);
        TagImportService importService = mock(TagImportService.class);
        AtomicReference<String> lookupThread = new AtomicReference<>();
        AtomicReference<String> importThread = new AtomicReference<>();
        TagImportResult result = successfulResult();

        when(mapper.selectById(7L)).thenAnswer(invocation -> {
            lookupThread.set(Thread.currentThread().getName());
            return enabledSource();
        });
        when(importService.importRows(eq("API_PULL"), eq(null), eq("http://93.184.216.34/tags"), any()))
                .thenAnswer(invocation -> {
                    importThread.set(Thread.currentThread().getName());
                    return result;
                });
        TagImportSourceService service = new TagImportSourceService(
                mapper, importService, new ObjectMapper(), webClient(new AtomicInteger()));

        Scheduler eventLoopLikeScheduler = Schedulers.newSingle("reactor-http-nio-test");
        try {
            StepVerifier.create(service.runAsync(7L).subscribeOn(eventLoopLikeScheduler))
                    .expectNext(result)
                    .verifyComplete();
        } finally {
            eventLoopLikeScheduler.dispose();
        }

        assertThat(lookupThread.get()).doesNotContain("reactor-http-nio-test");
        assertThat(importThread.get()).doesNotContain("reactor-http-nio-test");
    }

    private static WebClient.Builder webClient(AtomicInteger remoteCalls) {
        ExchangeFunction exchangeFunction = request -> {
            remoteCalls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            [
                              {
                                "idType": "USER_ID",
                                "idValue": "u001",
                                "tagCode": "level",
                                "tagValue": "gold",
                                "tagTime": "2026-06-03 10:00:00"
                              }
                            ]
                            """)
                    .build());
        };
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }

    private static TagImportSourceDO enabledSource() {
        TagImportSourceDO source = new TagImportSourceDO();
        source.setId(7L);
        source.setName("remote tag source");
        source.setEnabled(1);
        source.setUrl("http://93.184.216.34/tags");
        source.setMethod("GET");
        source.setRecordsPath("$");
        source.setFieldMapping("""
                {
                  "idType": "idType",
                  "idValue": "idValue",
                  "tagCode": "tagCode",
                  "tagValue": "tagValue",
                  "tagTime": "tagTime"
                }
                """);
        return source;
    }

    private static TagImportResult successfulResult() {
        TagImportResult result = new TagImportResult();
        result.setBatchId(99L);
        result.setStatus("SUCCESS");
        result.setTotalRows(1);
        result.setSuccessRows(1);
        result.setFailedRows(0);
        return result;
    }
}
