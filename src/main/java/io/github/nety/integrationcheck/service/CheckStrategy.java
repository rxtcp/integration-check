package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import io.github.nety.integrationcheck.domain.CheckType;

public interface CheckStrategy {

    CheckType getType();

    CheckResult execute(Check check);
}