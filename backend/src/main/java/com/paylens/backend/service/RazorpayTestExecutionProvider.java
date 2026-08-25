package com.paylens.backend.service;

import com.paylens.backend.dto.ExecutionCommand;
import com.paylens.backend.dto.ExecutionProviderResult;
import com.paylens.backend.model.ExecutionProvider;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
public class RazorpayTestExecutionProvider implements PaymentExecutionProvider {

    // Standard Razorpay Payment Gateway SDK only supports customer refund operations.
    // Outbound disbursements (VENDOR_PAYMENT, PAYROLL, TAX_PAYMENT) require RazorpayX Payouts API.
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            "REFUND", "CUSTOMER_REFUND"
    );

    private final String keyId;
    private final String keySecret;

    public RazorpayTestExecutionProvider(
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
    public ExecutionProviderResult execute(ExecutionCommand command) {
        String action = command.actionType() == null ? "" : command.actionType().toUpperCase();

        // 1. Explicit Action Support Mapping & Verification
        if (!SUPPORTED_ACTIONS.contains(action)) {
            if ("VENDOR_PAYMENT".equals(action) || "PAYROLL".equals(action) || "TAX_PAYMENT".equals(action)) {
                return ExecutionProviderResult.unsupported(
                        "Action type '" + command.actionType() + "' is an outbound disbursement. Standard Razorpay TEST Payment API only supports customer refunds (REFUND). Outbound vendor payments and payroll require RazorpayX Payouts."
                );
            }
            return ExecutionProviderResult.unsupported(
                    "Action type '" + command.actionType() + "' is not supported by standard Razorpay TEST payment API."
            );
        }

        // 2. Placeholder environment check for offline tests & dev environments without active keys
        if (keyId.contains("placeholder") || keyId.contains("mock")) {
            String mockReference = "rfnd_test_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 14);
            return ExecutionProviderResult.success(mockReference);
        }

        // 3. Official Razorpay TEST API SDK invocation for Refund
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject refundRequest = new JSONObject();

            BigDecimal amountInPaise = command.amount().multiply(new BigDecimal("100"));
            refundRequest.put("amount", amountInPaise.longValue());
            // Test payment ID reference for SDK refund contract
            refundRequest.put("payment_id", "pay_test_" + command.decisionId().replaceAll("[^a-zA-Z0-9]", ""));
            refundRequest.put("notes", new JSONObject()
                    .put("decisionId", command.decisionId())
                    .put("executionId", command.executionId()));

            Refund refund = razorpay.refunds.create(refundRequest);
            String providerReference = refund.get("id");
            if (providerReference == null || providerReference.isBlank()) {
                return ExecutionProviderResult.failure("RAZORPAY_NULL_REF", "Razorpay TEST API returned empty refund reference ID.");
            }
            return ExecutionProviderResult.success(providerReference);

        } catch (RazorpayException e) {
            return ExecutionProviderResult.failure("RAZORPAY_API_ERROR", e.getMessage() == null ? "Razorpay API error" : e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"))) {
                return ExecutionProviderResult.unknown("Network timeout communicating with Razorpay TEST gateway.");
            }
            return ExecutionProviderResult.failure("PROVIDER_CLIENT_ERROR", "Execution failed due to provider communication error.");
        }
    }
}
