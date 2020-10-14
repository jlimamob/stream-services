package com.backbase.stream.webclient;

import com.backbase.stream.webclient.logging.CustomLogger;
import com.backbase.stream.webclient.logging.LogFilters;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.channel.BootstrapHandlers;
import reactor.netty.http.client.HttpClient;


/**
 * DBS Web Client Configuration to be used by Stream Services that communicate with DBS.
 */
@Configuration
@Slf4j
public class DbsWebClientConfiguration {

    /**
     * Default Jackson Object Mapper.
     *
     * @param dateFormat Date Formatter to use
     * @return Preconfigured Jackson Object Mapper
     */
    @Bean
    public ObjectMapper objectMapper(DateFormat dateFormat) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(dateFormat);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * Date Format used by DBS.
     *
     * @return Date Formatter
     */
    @Bean
    public DateFormat dateFormat() {
        DateFormat dateFormat = new StdDateFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat;
    }

    /**
     * Default Reactive Web Client to be used when interacting with DBS Services. Requires OAuth2 client credentials set
     * in application.yml
     *
     * @param objectMapper                          The Jackson Object mapper to register serialization and deserialization json
     *                                              content.
     * @param reactiveOAuth2AuthorizedClientManager Client Manager managing OAuth2 tokens
     * @param builder                               THe Web Client Builder which is already preconfigured using MicroMeter
     *                                              instrumentation.
     * @return Preconfigured Web Client
     */
    @Bean
    public WebClient dbsWebClient(ObjectMapper objectMapper,
                                  ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager,
                                  WebClient.Builder builder) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(reactiveOAuth2AuthorizedClientManager);
        oauth2ClientFilter.setDefaultClientRegistrationId("dbs");

        boolean logRequestAndResponses = log.isDebugEnabled();
        HttpClient httpClient = HttpClient
            .create()
            .tcpConfiguration(tcpClient -> tcpClient.bootstrap(b -> BootstrapHandlers.updateLogSupport(b, new CustomLogger(DbsWebClientConfiguration.class))));

        builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON.toString())
            .filter(new CsrfClientExchangeFilterFunction())
            .filter(oauth2ClientFilter);


        // ensure correct exchange strategy is installed
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(clientDefaultCodecsConfigurer -> {
                Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON);
                Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON);

                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(encoder);
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(decoder);
            })
            .build();

        builder.exchangeStrategies(strategies);

        return builder.build();
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider() {
        return new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
        ReactiveClientRegistrationRepository reactiveClientRegistrationRepository,
        ReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider
    ) {

        InMemoryReactiveOAuth2AuthorizedClientService clientService = new
            InMemoryReactiveOAuth2AuthorizedClientService(reactiveClientRegistrationRepository);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager clientServiceReactiveOAuth2AuthorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            reactiveClientRegistrationRepository,
            clientService);
        clientServiceReactiveOAuth2AuthorizedClientManager.setAuthorizedClientProvider(reactiveOAuth2AuthorizedClientProvider);
        return clientServiceReactiveOAuth2AuthorizedClientManager;
    }

    @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties
                                                                                         properties) {
        Collection<ClientRegistration> values = OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values();
        List<ClientRegistration> registrations = new ArrayList<>(values);
        return new InMemoryReactiveClientRegistrationRepository(registrations);
    }


    private static class CsrfClientExchangeFilterFunction implements ExchangeFilterFunction {


        public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
            return next.exchange(request).flatMap(response -> {
                if (response.statusCode().is4xxClientError()) {
                    ResponseCookie csrfCookie = response.cookies().getFirst("XSRF-TOKEN");
                    if (csrfCookie != null) {
                        ClientRequest retryRequest = ClientRequest.from(request)
                            .headers(httpHeaders -> httpHeaders.set("X-XSRF-TOKEN", csrfCookie.getValue()))
                            .cookies(cookies -> cookies.add("XSRF-TOKEN", csrfCookie.getValue()))
                            .build();
                        return next.exchange(retryRequest);
                    }
                }
                return Mono.just(response);
            });
        }
    }


}
