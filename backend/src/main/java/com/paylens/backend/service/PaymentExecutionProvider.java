package com.paylens.backend.service;

import com.paylens.backend.dto.ExecutionCommand;
import com.paylens.backend.dto.ExecutionProviderResult;
import com.paylens.backend.model.ExecutionProvider;

public interface PaymentExecutionProvider {
    ExecutionProviderResult execute(ExecutionCommand command);
    ExecutionProvider getProviderType();
}
