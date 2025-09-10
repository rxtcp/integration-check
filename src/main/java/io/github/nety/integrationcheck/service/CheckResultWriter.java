package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.CheckResult;

public interface CheckResultWriter {

    CheckResult saveResult(CheckResult results);
}