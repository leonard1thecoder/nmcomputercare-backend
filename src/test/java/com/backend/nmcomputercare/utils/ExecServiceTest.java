package com.backend.nmcomputercare.utils;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ExecServiceTest {

    @Test
    void execRunsServiceOnVirtualThreadAndPropagatesMdc() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Harness harness = new Harness(executor, new ExceptionAdvice());
            MDC.put("correlationId", "test-correlation-id");

            @SuppressWarnings("unchecked")
            List<CapturedResponse> response = (List<CapturedResponse>) harness.run(
                    (serviceName, requestContract) -> List.of(new CapturedResponse(
                            serviceName,
                            Thread.currentThread().isVirtual(),
                            MDC.get("correlationId"))));

            assertThat(response).singleElement().satisfies(item -> {
                assertThat(item.serviceName()).isEqualTo("captureThread");
                assertThat(item.virtualThread()).isTrue();
                assertThat(item.correlationId()).isEqualTo("test-correlation-id");
            });
        } finally {
            MDC.clear();
        }
    }

    private static final class Harness extends ExecService {
        private Harness(ExecutorService exec, ExceptionAdvice advice) {
            super(exec, advice);
        }

        private List<? extends ResponseContract> run(ExecuteService service) {
            return exec(service, "captureThread", new RequestContract() {});
        }
    }

    private record CapturedResponse(
            String serviceName,
            boolean virtualThread,
            String correlationId) implements ResponseContract {
    }
}
