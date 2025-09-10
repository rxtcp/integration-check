package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;

import java.util.List;

public interface CheckReader {

    List<Check> readChecks();
}
