package de.muenchen.refarch.gateway.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static de.muenchen.refarch.gateway.TestConstants.SPRING_TEST_PROFILE;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import de.muenchen.refarch.gateway.ApiGatewayApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = { ApiGatewayApplication.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(SPRING_TEST_PROFILE)
@AutoConfigureWireMock
@TestPropertySource(
        properties = {
                "config.map5xxto400=true",
        }
)
public class GlobalBackend5xxTo400MapperTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser
    public void backendError500() {

        stubFor(get(urlEqualTo("/remote"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeaders(new HttpHeaders(
                                new HttpHeader("Content-Type", "application/json"),
                                new HttpHeader("WWW-Authenticate", "Bearer realm=\"Access to the staging site\", charset=\"UTF-8\""),
                                new HttpHeader("Expires", "Wed, 21 Oct 2099 07:28:06 GMT")))
                        .withBody("{ \"testkey\" : \"testvalue\" }")));

        webTestClient.get().uri("/api/refarch-gateway-backend-service/remote").exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectHeader().valueMatches("Content-Type", "application/json")
                .expectHeader().doesNotExist("WWW-Authenticate")
                .expectHeader().valueMatches("Expires", "0")
                .expectBody()
                .jsonPath("$.status").isEqualTo("400")
                .jsonPath("$.error").isEqualTo("Bad Request");
    }

    @Test
    @WithMockUser
    public void backendError200() {

        stubFor(get(urlEqualTo("/remote"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeaders(new HttpHeaders(
                                new HttpHeader("Content-Type", "application/json"),
                                new HttpHeader("WWW-Authenticate", "Bearer realm=\"Access to the staging site\", charset=\"UTF-8\""),
                                new HttpHeader("Expires", "Wed, 21 Oct 2099 07:28:06 GMT")))
                        .withBody("{ \"testkey\" : \"testvalue\" }")));

        webTestClient.get().uri("/api/refarch-gateway-backend-service/remote").exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectHeader().valueMatches("Content-Type", "application/json")
                .expectHeader().doesNotExist("WWW-Authenticate")
                .expectHeader().valueMatches("Expires", "0")
                .expectBody()
                .jsonPath("$.testkey").isEqualTo("testvalue");
    }

}
