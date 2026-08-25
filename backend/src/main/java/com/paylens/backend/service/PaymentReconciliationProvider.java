package com.paylens.backend.service;

import com.paylens.backend.dto.ReconciliationCommand;
import com.paylens.backend.dto.ReconciliationProviderResult;
import com.paylens.backend.model.ExecutionProvider;

public interface PaymentReconciliationProvider {
    ReconciliationProviderResult reconcile(ReconciliationCommand command);
    ExecutionProvider getProviderType();
}
