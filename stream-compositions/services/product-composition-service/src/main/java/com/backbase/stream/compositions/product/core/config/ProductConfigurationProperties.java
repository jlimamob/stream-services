package com.backbase.stream.compositions.product.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.product")
public class ProductConfigurationProperties {
    private String integrationBaseUrl = "http://product-ingestion-integration:8080";
    private Chains chains;
    private Events events;
    private Cursor cursor;

    @Data
    @NoArgsConstructor
    public static class Events {
        private Boolean enableCompleted = Boolean.FALSE;
        private Boolean enableFailed = Boolean.FALSE;
    }

    @Data
    @NoArgsConstructor
    public static class Cursor {
        private Boolean enabled = Boolean.FALSE;
        private String baseUrl = "http://product-cursor:9000";
    }

    @Data
    @NoArgsConstructor
    public static class Chains {
        private TransactionComposition transactionComposition;
    }
    @Data
    public static abstract class BaseComposition {
        private Boolean enableOnComplete = Boolean.FALSE;
        private Boolean enableOnFailure = Boolean.FALSE;
        private String baseUrl = "http://localhost:9003/";
        private Boolean async = Boolean.FALSE;
    }
    @NoArgsConstructor
    public static class TransactionComposition extends BaseComposition {}
}
