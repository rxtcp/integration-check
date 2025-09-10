package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;

public interface CheckProcessor {

    CheckResult process(Check checks);
}