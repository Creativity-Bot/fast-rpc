package com.fastrpc.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Slf4j
class HelloControllerTest {

    @Autowired
    private RestTestClient client;

    @Test
    void browserRequestIsForwardedThroughRpc() {
        EntityExchangeResult<String> result = client.get()
                .uri("/hello?name=faster-rpc")
                .exchange()
                .expectBody(String.class)
                .returnResult();
        log.debug("test result is {}", result.getResponseBody());
        assertThat(result.getStatus().is2xxSuccessful()).isTrue();
        assertThat(result.getResponseBody()).isEqualTo("hello faster-rpc");
    }
}
