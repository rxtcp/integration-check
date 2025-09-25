package io.github.nety.integrationcheck.configuration;

import org.springframework.core.task.TaskDecorator;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Ставит имени потока вид "<prefix>-{i}/{n}" на время задачи.
 * Индекс {i} немедленно переиспользуется после завершения задачи/потока.
 *
 * Потокобезопасен. Работает и с виртуальными потоками JDK 21.
 */
public final class StableIndexThreadNameTaskDecorator implements TaskDecorator {

    private final String prefix;             // Нормализованный: с завершающим '-'
    private final int capacity;              // n
    private final PriorityBlockingQueue<Integer> free; // доступные индексы (min-first)
    private final Semaphore permits;         // ограничитель «n одновременно»

    public StableIndexThreadNameTaskDecorator(String prefix, int capacity) {
        this.prefix = prefix;
        this.capacity = capacity;
        this.free = new PriorityBlockingQueue<>(capacity);
        for (int i = 1; i <= capacity; i++) {
            free.add(i);
        }
        // fair=true — более предсказуемое распределение под нагрузкой
        this.permits = new Semaphore(capacity, true);
    }

    @Override
    public Runnable decorate(Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            final Thread t = Thread.currentThread();
            final String originalName = t.getName();

            int idx = -1;
            boolean renamed = false;

            try {
                // Блокируем старт, пока не появится свободный индекс
                permits.acquire();                // interruptible
                idx = free.take();                // минимальный доступный индекс
                t.setName(prefix + idx + "/" + capacity);
                renamed = true;
                task.run();
            } catch (InterruptedException ie) {
                // Сохраняем семантику прерывания и выполняем задачу без переименования
                Thread.currentThread().interrupt();
                task.run();
            } finally {
                if (renamed) {
                    t.setName(originalName);
                }
                if (idx != -1) {
                    free.offer(idx);             // вернуть индекс для переиспользования
                    permits.release();
                }
            }
        };
    }
}
