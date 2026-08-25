package com.paylens.backend.service;

import com.paylens.backend.dto.ReconciliationCommand;
import com.paylens.backend.dto.ReconciliationProviderResult;
import com.paylens.backend.model.ExecutionProvider;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayTestReconciliationProvider implements PaymentReconciliationProvider {

    private final String keyId;
    private final String keySecret;

    public RazorpayTestReconciliationProvider(
            @Value("${razorpay.key.id:${RAZORPAY_KEY_ID:rzp_test_placeholder}}") String keyId,
            @Value("${razorpay.key.secret:${RAZORPAY_KEY_SECRET:placeholder_secret}}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public ExecutionProvider getProviderType() {
        return ExecutionProvider.RAZORPAY_TEST;
    }

    @Override
    public ReconciliationProviderResult reconcile(ReconciliationCommand command) {
        String ref = command.providerReference();
        if (ref == null || ref.isBlank()) {
            return ReconciliationProviderResult.notFound("Provider reference is missing on execution record.");
        }

        // Placeholder environment check for offline tests & dev environments without active keys
        if (keyId.contains("placeholder") || keyId.contains("mock")) {
            if (ref.contains("notfound")) {
                return ReconciliationProviderResult.notFound("Refund ID " + ref + " not found on Razorpay server.");
            }
            if (ref.contains("fail")) {
                return ReconciliationProviderResult.confirmedFailure("failed", "REFUND_FAILED", "Razorpay refund transaction failed.");
            }
            if (ref.contains("pending")) {
                return ReconciliationProviderResult.stillProcessing("pending");
            }
            if (ref.contains("timeout")) {
                return ReconciliationProviderResult.unknown("GATEWAY_TIMEOUT", "Provider communication timed out during status query.");
            }
            return ReconciliationProviderResult.confirmedSuccess("processed");
        }

        // Official Razorpay TEST API SDK Fetch Invocation
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            Refund refund = razorpay.refunds.fetch(ref);
            String status = refund.get("status");

            if ("processed".equalsIgnoreCase(status)) {
                return ReconciliationProviderResult.confirmedSuccess(status);
            }
            if ("failed".equalsIgnoreCase(status)) {
                return ReconciliationProviderResult.confirmedFailure(status, "RAZORPAY_REFUND_FAILED", "Razorpay refund failed.");
            }
            if ("pending".equalsIgnoreCase(status) || "processing".equalsIgnoreCase(status)) {
                return ReconciliationProviderResult.stillProcessing(status);
            }
            return ReconciliationProviderResult.unknown("UNRECOGNIZED_STATUS", "Provider returned status: " + status);

        } catch (RazorpayException e) {
            if (e.getMessage() != null && e.getMessage().contains("BAD_REQUEST_ERROR")) {
                return ReconciliationProviderResult.notFound("Reference " + ref + " not found on Razorpay server.");
            }
            return ReconciliationProviderResult.unknown("RAZORPAY_API_ERROR", e.getMessage() == null ? "Razorpay API error" : e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"))) {
                return ReconciliationProviderResult.unknown("GATEWAY_TIMEOUT", "Network timeout communicating with Razorpay TEST gateway.");
            }
            return ReconciliationProviderResult.unknown("PROVIDER_CLIENT_ERROR", "Reconciliation failed due to provider communication error.");
        }
    }
}
