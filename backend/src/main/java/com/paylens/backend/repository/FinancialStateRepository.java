package com.paylens.backend.repository;

import com.paylens.backend.model.FinancialAccount;
import com.paylens.backend.model.Obligation;
import com.paylens.backend.model.Transaction;
import java.util.List;

public interface FinancialStateRepository {
    FinancialAccount getAccount();

    List<Transaction> getTransactions();

    List<Obligation> getObligations();
}
