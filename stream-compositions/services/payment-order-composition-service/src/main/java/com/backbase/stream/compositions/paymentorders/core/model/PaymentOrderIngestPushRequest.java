package com.backbase.stream.compositions.paymentorders.core.model;

import com.backbase.dbs.paymentorder.api.service.v2.model.PaymentOrderPostRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentOrderIngestPushRequest {

    private String memberNumber;
    private String internalUserId;
    private String legalEntityInternalId;
    private String legalEntityExternalId;
    private String serviceAgreementInternalId;
    private List<PaymentOrderPostRequest> paymentOrders;
}
