package com.backbase.stream.compositions.legalentity.core.config;

import com.backbase.stream.compositions.legalentity.integration.client.LegalEntityIntegrationApi;
import com.backbase.stream.compositions.product.ApiClient;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties({
        LegalEntityConfigurationProperties.class
})
public class LegalEntityConfiguration {

  private final LegalEntityConfigurationProperties legalEntityConfigurationProperties;

  @Bean
  @Primary
  public LegalEntityIntegrationApi legalEntityIntegrationApi(
      com.backbase.stream.compositions.legalentity.integration.ApiClient legalEntityClient2) {
    return new LegalEntityIntegrationApi(legalEntityClient2);
  }

  @Bean
  @Primary
  public ProductCompositionApi productCompositionApi(ApiClient productClient) {
    return new ProductCompositionApi(productClient);
  }

  @Bean
  public ApiClient productClient() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(
        legalEntityConfigurationProperties.getChains().getProductComposition().getBaseUrl());

    return apiClient;
  }

  @Bean
  public com.backbase.stream.compositions.legalentity.integration.ApiClient legalEntityClient1() {
    com.backbase.stream.compositions.legalentity.integration.ApiClient apiClient =
        new com.backbase.stream.compositions.legalentity.integration.ApiClient();
    apiClient.setBasePath(legalEntityConfigurationProperties.getIntegrationBaseUrl());

    return apiClient;
  }

  @Bean
  @Primary
  WebClient dbsWebclient() {
    WebClient.Builder builder = WebClient.builder();
    builder.defaultHeader("Content-Type", new String[]{MediaType.APPLICATION_JSON.toString()});
    builder.defaultHeader("Accept", new String[]{MediaType.APPLICATION_JSON.toString()});
    return builder.build();
  }

  @Bean
  public com.backbase.stream.compositions.legalentity.integration.ApiClient legalEntityClient2(
          WebClient dbsWebClient,
          ObjectMapper objectMapper, DateFormat dateFormat) {
    com.backbase.stream.compositions.legalentity.integration.ApiClient apiClient =
            new com.backbase.stream.compositions.legalentity.integration.ApiClient(
                    dbsWebClient, objectMapper, dateFormat);
    apiClient.setBasePath(legalEntityConfigurationProperties.getIntegrationBaseUrl());

    return apiClient;
  }

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http.csrf().disable().build();
  }
}
