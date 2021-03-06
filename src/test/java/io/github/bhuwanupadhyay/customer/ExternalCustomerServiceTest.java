package io.github.bhuwanupadhyay.customer;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWireMock(port = 6766)
class ExternalCustomerServiceTest {

  @Autowired private ExternalCustomerService service;

  @BeforeEach
  void setUp() {}

  @Test
  @SneakyThrows
  void givenNotIgnoredException_whenFetch_thenShouldOpenTheCircuit() {

    // given
    String customerId = "ITM00001";
    UrlPattern urlPattern = urlEqualTo("/api/v1/customers/" + customerId);
    stubFor(
        get(urlPattern)
            .willReturn(
                serverError()
                    .withHeader("Content-Type", "application/json")
                    .withBody("Not found.")));

    // when

    assertThrows(
        HttpServerErrorException.InternalServerError.class,
        () -> service.fetchCustomer(customerId));

    Thread.sleep(1000);

    HystrixRuntimeException e =
        assertThrows(HystrixRuntimeException.class, () -> service.fetchCustomer(customerId));

    // then
    assertEquals("fetchCustomerCommand short-circuited and fallback failed.", e.getMessage());
    verify(1, newRequestPattern(RequestMethod.GET, urlPattern));
  }

  @Test
  void givenIgnoredException_whenFetch_thenShouldInvokeDownstreamAPI() {

    // given
    String customerId = "ITM00001";
    UrlPattern urlPattern = urlEqualTo("/api/v1/customers/" + customerId);
    stubFor(
        get(urlPattern)
            .willReturn(
                notFound().withHeader("Content-Type", "application/json").withBody("Not found.")));

    // when
    for (int i = 1; i < 5; i++) {
      assertThrows(HttpClientErrorException.class, () -> service.fetchCustomer(customerId));
    }

    // then
    verify(4, newRequestPattern(RequestMethod.GET, urlPattern));
  }

  @Test
  void givenOkResponse_whenFetch_thenShouldReturnCustomer() {
    // given
    String customerId = "ITM00001";
    stubFor(
        get(urlEqualTo("/api/v1/customers/" + customerId))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("customer.json")));

    // when
    Customer customer = this.service.fetchCustomer(customerId);

    // then
    assertEquals("Bhuwan", customer.getName());
  }
}

/*
   for (int i = 0; i < 25; i++) {
     //      Thread.sleep(500);
     // HystrixCommand<String> command = new GetOrderCircuitBreakerCommand("testCircuitBreaker");
     /// String result = command.execute();
     // In this example, from the 11th time, the fuse starts to open.
     //      System.out.println(
     //          "call times:"
     //              + (i + 1)
     //              + "   result:"
     //              + result
     //              + " isCircuitBreakerOpen: "
     //              + command.isCircuitBreakerOpen());
     // After 5 seconds in this example, the fuse tries to close and release new requests.
     // Assertions.assertThatExceptionOfType(PrimaryException.class).isThrownBy(() ->
     // externalInterface.primary());
     // ... and the metrics have been calculated
     // wait at least hystrix.command.default.metrics.healthSnapshot.intervalInMilliseconds to
     // calculate metrics
     // the default is 500 ms
     // Thread.sleep(1000);

     // then ...
     // ... a runtime exception with circuit breaker open is raised
     // Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
     // service.fetchCustomer(customerId));

     // safeFetchCustomer(customerId);


   }
*/
