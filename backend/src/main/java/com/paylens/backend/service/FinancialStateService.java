package com.paylens.backend.service;

import com.paylens.backend.dto.DashboardResponse;
import com.paylens.backend.dto.FinancialAccountResponse;
import com.paylens.backend.dto.FinancialStateResponse;
import com.paylens.backend.dto.FinancialSummaryResponse;
import com.paylens.backend.dto.ObligationResponse;
import com.paylens.backend.dto.TransactionResponse;
import com.paylens.backend.model.FinancialAccount;
import com.paylens.backend.model.Obligation;
import com.paylens.backend.model.ObligationStatus;
import com.paylens.backend.repository.FinancialStateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinancialStateService {
    private final FinancialStateRepository financialStateRepository;

    public FinancialStateService(FinancialStateRepository financialStateRepository) {
        this.financialStateRepository = financialStateRepository;
    }

    public FinancialStateResponse getFinancialState() {
        var account = financialStateRepository.getAccount();
        var transactions = financialStateRepository.getTransactions();
        var obligations = financialStateRepository.getObligations();
        return new FinancialStateResponse(
                toResponse(account),
                transactions.stream().map(this::toResponse).toList(),
                obligations.stream().map(this::toResponse).toList(),
                createSummary(account, obligations));
    }

    public DashboardResponse getDashboard() {
        var account = financialStateRepository.getAccount();
        var summary = createSummary(account, financialStateRepository.getObligations());
        return new DashboardResponse(
                account.currency(), account.currentBalance(), summary.upcomingObligations(), account.safetyReserve(),
                summary.availableLiquidity(), summary.remainingAfterObligations(), summary.safetyBuffer());
    }

    private FinancialSummaryResponse createSummary(FinancialAccount account, List<Obligation> obligations) {
        BigDecimal upcomingObligations = BigDecimal.ZERO;
        for (var obligation : obligations) {
            if (obligation.status() == ObligationStatus.UPCOMING || obligation.status() == ObligationStatus.DUE) {
                upcomingObligations = upcomingObligations.add(obligation.amount());
            }
        }

        BigDecimal availableLiquidity = account.currentBalance().subtract(account.safetyReserve());
        BigDecimal remainingAfterObligations = account.currentBalance().subtract(upcomingObligations);
        BigDecimal safetyBuffer = remainingAfterObligations.subtract(account.safetyReserve());
        return new FinancialSummaryResponse(upcomingObligations, availableLiquidity, remainingAfterObligations, safetyBuffer);
    }

    private FinancialAccountResponse toResponse(FinancialAccount account) {
        return new FinancialAccountResponse(
                account.id(),
                account.accountName(),
                account.currency(),
                account.currentBalance(),
                account.safetyReserve());
    }

    private TransactionResponse toResponse(com.paylens.backend.model.Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.type(),
                transaction.amount(),
                transaction.description(),
                transaction.timestamp(),
                transaction.status());
    }

    private ObligationResponse toResponse(Obligation obligation) {
        return new ObligationResponse(
                obligation.id(),
                obligation.type(),
                obligation.description(),
                obligation.amount(),
                obligation.dueDate(),
                obligation.status());
    }
}
