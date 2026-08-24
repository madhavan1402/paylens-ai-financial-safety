package com.paylens.backend.repository;

import com.paylens.backend.model.FinancialAccount;
import com.paylens.backend.model.Obligation;
import com.paylens.backend.model.ObligationStatus;
import com.paylens.backend.model.ObligationType;
import com.paylens.backend.model.Transaction;
import com.paylens.backend.model.TransactionStatus;
import com.paylens.backend.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFinancialStateRepository implements FinancialStateRepository {
    private static final String INR = "INR";
    private static final FinancialAccount ACCOUNT = new FinancialAccount(
            "account-merchant-primary", "PayLens Merchant Operating Account", INR,
            new BigDecimal("840000"), new BigDecimal("100000"));

    private static final List<Transaction> TRANSACTIONS = List.of(
            new Transaction("txn-001", TransactionType.PAYMENT_IN, new BigDecimal("275000"),
                    "Marketplace settlement", OffsetDateTime.parse("2026-08-04T10:00:00+05:30"), TransactionStatus.COMPLETED),
            new Transaction("txn-002", TransactionType.PAYMENT_IN, new BigDecimal("180000"),
                    "Wholesale customer payment", OffsetDateTime.parse("2026-08-08T14:30:00+05:30"), TransactionStatus.COMPLETED),
            new Transaction("txn-003", TransactionType.VENDOR_PAYMENT, new BigDecimal("120000"),
                    "Inventory supplier payment", OffsetDateTime.parse("2026-08-12T11:15:00+05:30"), TransactionStatus.COMPLETED),
            new Transaction("txn-004", TransactionType.REFUND, new BigDecimal("18000"),
                    "Customer refund batch", OffsetDateTime.parse("2026-08-15T16:45:00+05:30"), TransactionStatus.COMPLETED));

    private static final List<Obligation> OBLIGATIONS = List.of(
            new Obligation("obl-001", ObligationType.PAYROLL, "August payroll", new BigDecimal("300000"), LocalDate.parse("2026-08-28"), ObligationStatus.DUE),
            new Obligation("obl-002", ObligationType.VENDOR, "Inventory vendor invoices", new BigDecimal("150000"), LocalDate.parse("2026-08-30"), ObligationStatus.UPCOMING),
            new Obligation("obl-003", ObligationType.TAX, "GST payment", new BigDecimal("100000"), LocalDate.parse("2026-09-05"), ObligationStatus.UPCOMING),
            new Obligation("obl-004", ObligationType.REFUND, "Expected customer refunds", new BigDecimal("70000"), LocalDate.parse("2026-09-10"), ObligationStatus.UPCOMING));

    @Override
    public FinancialAccount getAccount() {
        return ACCOUNT;
    }

    @Override
    public List<Transaction> getTransactions() {
        return TRANSACTIONS;
    }

    @Override
    public List<Obligation> getObligations() {
        return OBLIGATIONS;
    }
}
