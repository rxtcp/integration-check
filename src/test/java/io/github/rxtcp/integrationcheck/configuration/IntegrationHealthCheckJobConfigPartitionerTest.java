package io.github.rxtcp.integrationcheck.configuration;

import io.github.rxtcp.integrationcheck.service.reader.CheckReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link IntegrationHealthCheckJobConfig#checkIdPartitioner(CheckReader)}.
 * <p>
 * Идея: для каждого ID из источника должен создаваться отдельный partition с ключом "check-{id}"
 * и значением ExecutionContext, содержащим "checkId" с тем же значением.
 */
@DisplayName("IntegrationHealthCheckJobConfig.checkIdPartitioner(...)")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckJobConfigPartitionerTest {

    @ParameterizedTest(name = "[{index}] gridSize={0}")
    @ValueSource(ints = {1, 3, 10})
    void should_build_one_partition_per_id_regardless_of_grid_size(int gridSize) {
        // given
        final var checkReader = mock(CheckReader.class);
        when(checkReader.findDueIds()).thenReturn(List.of(1L, 2L, 5L));

        final var cfg = new IntegrationHealthCheckJobConfig();
        final Partitioner partitioner = cfg.checkIdPartitioner(checkReader);

        // when
        final Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // then
        assertThat(partitions)
                .as("ожидаем по одному partition на каждый ID")
                .hasSize(3)
                .containsKeys("check-1", "check-2", "check-5");

        assertThat(partitions.get("check-1").getLong("checkId")).isEqualTo(1L);
        assertThat(partitions.get("check-2").getLong("checkId")).isEqualTo(2L);
        assertThat(partitions.get("check-5").getLong("checkId")).isEqualTo(5L);

        verify(checkReader, times(1)).findDueIds();
        verifyNoMoreInteractions(checkReader);
    }

    @Test
    void should_return_empty_partitions_when_reader_returns_no_ids() {
        // given
        final var checkReader = mock(CheckReader.class);
        when(checkReader.findDueIds()).thenReturn(List.of());

        final var cfg = new IntegrationHealthCheckJobConfig();
        final Partitioner partitioner = cfg.checkIdPartitioner(checkReader);

        // when
        final Map<String, ExecutionContext> partitions = partitioner.partition(8);

        // then
        assertThat(partitions).isEmpty();
        verify(checkReader, times(1)).findDueIds();
        verifyNoMoreInteractions(checkReader);
    }
}