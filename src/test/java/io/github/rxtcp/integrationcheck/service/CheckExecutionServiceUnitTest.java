package io.github.rxtcp.integrationcheck.service;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.enums.HttpMethod;
import io.github.rxtcp.integrationcheck.mapper.CheckMapper;
import io.github.rxtcp.integrationcheck.service.processor.CheckProcessor;
import io.github.rxtcp.integrationcheck.service.reader.CheckReader;
import io.github.rxtcp.integrationcheck.service.writer.CheckResultWriter;
import io.github.rxtcp.integrationcheck.service.writer.CheckWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.FAILED;
import static io.github.rxtcp.integrationcheck.enums.FailureReason.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("CheckExecutionService — сценарии выполнения и обработка ошибок")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckExecutionServiceUnitTest {

    // === Константы для стабильности тестов (никаких now() в проверках) =========================
    private static final long CHECK_ID = 42L;
    private static final long RESULT_ID = 10L;

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    private static final LocalDateTime NEXT_RUN_AT = NOW.plusMinutes(5);
    private static final LocalDateTime NEXT_DTO_RUN_AT = NOW.plusMinutes(1);

    // === Моки зависимостей =====================================================================
    @Mock
    CheckMapper checkMapper;
    @Mock
    CheckReader checkReader;
    @Mock
    CheckProcessor checkProcessor;
    @Mock
    CheckWriter checkWriter;
    @Mock
    CheckResultWriter checkResultWriter;

    @InjectMocks
    CheckExecutionService service;

    // === Хелперы DTO/Entity (фиксированные значения) ===========================================
    private static RestApiProfileDto profileDto() {
        return new RestApiProfileDto(
                100L, 200L, "https://example.org", HttpMethod.GET,
                5, null, null, 200
        );
    }

    private static CheckDto checkDto() {
        return new CheckDto(
                CHECK_ID, "name", "desc", true, 5,
                NEXT_DTO_RUN_AT, CheckType.REST_API, profileDto()
        );
    }

    private static Check checkEntity(long id) {
        return Check.builder()
                .id(id)
                .name("entity")
                .description("desc")
                .enabled(true)
                .runIntervalMin(5)
                .nextRunAt(NEXT_RUN_AT)
                .type(CheckType.REST_API)
                .build();
    }

    private static CheckResult processingResult(Long id) {
        return CheckResult.builder().id(id).status(CheckRunStatus.PROCESSING).build();
    }

    // === Тесты =================================================================================

    @Test
    void should_call_dependencies_in_order_and_propagate_data_on_happy_path() {
        // given
        Check entity = checkEntity(CHECK_ID);
        CheckResult started = processingResult(RESULT_ID);
        CheckDto dto = checkDto();
        CheckResultDto resultDto = new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok");
        CheckResult finished = CheckResult.builder()
                .id(RESULT_ID)
                .status(CheckRunStatus.SUCCEEDED)
                .details("ok")
                .build();
        Check updated = checkEntity(CHECK_ID);

        when(checkReader.findWithProfileById(CHECK_ID)).thenReturn(entity);
        when(checkResultWriter.recordProcessStart(entity)).thenReturn(started);
        when(checkMapper.toDto(entity)).thenReturn(dto);
        when(checkProcessor.process(dto)).thenReturn(resultDto);
        when(checkResultWriter.recordProcessEnd(started, resultDto)).thenReturn(finished);
        when(checkWriter.updateNextExecutionTime(entity, finished)).thenReturn(updated);

        // when
        assertThatNoException().isThrownBy(() -> service.execute(CHECK_ID));

        // then
        InOrder inOrder = inOrder(checkReader, checkResultWriter, checkMapper, checkProcessor, checkWriter);
        inOrder.verify(checkReader).findWithProfileById(CHECK_ID);
        inOrder.verify(checkResultWriter).recordProcessStart(entity);
        inOrder.verify(checkMapper).toDto(entity);
        inOrder.verify(checkProcessor).process(dto);
        inOrder.verify(checkResultWriter).recordProcessEnd(started, resultDto);
        inOrder.verify(checkWriter).updateNextExecutionTime(entity, finished);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void should_record_failed_result_when_processor_throws_and_still_finalize() {
        // given
        long id = 7L;
        Check entity = checkEntity(id);
        CheckResult started = processingResult(1L);
        CheckDto dto = checkDto();

        when(checkReader.findWithProfileById(id)).thenReturn(entity);
        when(checkResultWriter.recordProcessStart(entity)).thenReturn(started);
        when(checkMapper.toDto(entity)).thenReturn(dto);
        when(checkProcessor.process(dto)).thenThrow(new IllegalStateException("boom"));
        when(checkWriter.updateNextExecutionTime(any(), any())).thenReturn(entity);

        ArgumentCaptor<CheckResultDto> dtoCaptor = ArgumentCaptor.forClass(CheckResultDto.class);
        when(checkResultWriter.recordProcessEnd(any(CheckResult.class), any(CheckResultDto.class)))
                .thenReturn(CheckResult.builder().id(1L).status(FAILED).build());

        // when
        service.execute(id);

        // then — сервис должен зафиксировать FAILED с причиной ERROR и подробностями
        verify(checkResultWriter).recordProcessEnd(same(started), dtoCaptor.capture());
        CheckResultDto failed = dtoCaptor.getValue();
        assertThat(failed.status()).isEqualTo(FAILED);
        assertThat(failed.failureReason()).isEqualTo(ERROR);
        assertThat(failed.details()).contains("boom");

        verify(checkWriter).updateNextExecutionTime(same(entity), any(CheckResult.class));
    }

    @Test
    void should_swallow_reader_exception_and_stop_pipeline() {
        // given
        when(checkReader.findWithProfileById(anyLong())).thenThrow(new RuntimeException("read-fail"));

        // when / then — ошибка чтения не должна «протечь» наружу, остальные взаимодействия — отсутствуют
        assertThatNoException().isThrownBy(() -> service.execute(999L));

        verify(checkReader).findWithProfileById(999L);
        verifyNoInteractions(checkMapper, checkProcessor, checkWriter, checkResultWriter);
    }

    @Test
    void should_catch_finalize_exception_and_not_propagate() {
        // given
        long id = 55L;
        Check entity = checkEntity(id);
        CheckResult started = processingResult(5L);
        CheckDto dto = checkDto();
        CheckResultDto ok = new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok");

        when(checkReader.findWithProfileById(id)).thenReturn(entity);
        when(checkResultWriter.recordProcessStart(entity)).thenReturn(started);
        when(checkMapper.toDto(entity)).thenReturn(dto);
        when(checkProcessor.process(dto)).thenReturn(ok);
        when(checkResultWriter.recordProcessEnd(started, ok))
                .thenReturn(CheckResult.builder().id(5L).status(CheckRunStatus.SUCCEEDED).build());
        when(checkWriter.updateNextExecutionTime(any(), any()))
                .thenThrow(new IllegalStateException("update fail"));

        // when / then — исключение финализации не должно «протечь» наружу
        assertThatNoException().isThrownBy(() -> service.execute(id));

        verify(checkWriter).updateNextExecutionTime(eq(entity), any(CheckResult.class));
    }
}