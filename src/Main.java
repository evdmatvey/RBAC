import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static final int THREAD_COUNT = 5;
    private static final int CALCULATION_LENGTH = 30;
    private static final int STEP_DELAY_MS = 200;

    private static final ProgressBar[] progressBars = new ProgressBar[THREAD_COUNT];
    private static final AtomicInteger threadCounter = new AtomicInteger(1);
    private static final ReentrantLock displayLock = new ReentrantLock();

    public static void main(String[] args) {
        System.out.println("Многопоточный расчёт начат...");
        System.out.println("Количество потоков: " + THREAD_COUNT);
        System.out.println("Длина расчёта: " + CALCULATION_LENGTH + " шагов");
        System.out.println("Задержка между шагами: " + STEP_DELAY_MS + " мс\n");

        for (int i = 0; i < THREAD_COUNT; i++) {
            System.out.println();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> new CalculationTask(index).run());
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\nВсе потоки завершили работу!");
    }

    private static void updateDisplay() {
        displayLock.lock();
        try {
            System.out.print("\u001B[s");
            System.out.print("\u001B[" + (THREAD_COUNT + 4) + "A");

            for (int i = 0; i < THREAD_COUNT; i++) {
                System.out.print("\u001B[2K");
                if (progressBars[i] != null) {
                    System.out.println(progressBars[i].getDisplayString());
                } else {
                    System.out.println();
                }
            }

            System.out.print("\u001B[u");
            System.out.flush();
        } finally {
            displayLock.unlock();
        }
    }

    static class ProgressBar {
        final int threadNumber;
        final long threadId;
        final char[] barChars;
        volatile int currentStep;
        volatile long startTime;
        volatile long endTime;
        final int index;

        ProgressBar(int threadNumber, long threadId, int index) {
            this.threadNumber = threadNumber;
            this.threadId = threadId;
            this.index = index;
            this.barChars = new char[CALCULATION_LENGTH];
            this.currentStep = 0;
            this.startTime = System.currentTimeMillis();

            for (int i = 0; i < CALCULATION_LENGTH; i++) {
                barChars[i] = ' ';
            }
        }

        void updateProgress() {
            if (currentStep < CALCULATION_LENGTH) {
                synchronized (this) {
                    if (currentStep < CALCULATION_LENGTH) {
                        barChars[currentStep] = '█';
                        currentStep++;
                    }
                }
                updateDisplay();
            }
        }

        synchronized void complete() {
            if (endTime == 0) {
                endTime = System.currentTimeMillis();

                while (currentStep < CALCULATION_LENGTH) {
                    barChars[currentStep] = '█';
                    currentStep++;
                }

                updateDisplay();
            }
        }

        String getDisplayString() {
            long duration = (endTime != 0 ? endTime : System.currentTimeMillis()) - startTime;
            String status = endTime != 0 ? "ГОТОВО" : "ВЫПОЛНЯЕТСЯ";
            return String.format("Поток #%-2d [ID: %-6d] [%s] %s Время: %4d мс",
                    threadNumber,
                    threadId,
                    new String(barChars),
                    status,
                    duration);
        }
    }

    static class CalculationTask {
        private final int index;

        CalculationTask(int index) {
            this.index = index;
        }

        public void run() {
            int threadNumber = threadCounter.getAndIncrement();
            long threadId = Thread.currentThread().getId();

            ProgressBar progressBar = new ProgressBar(threadNumber, threadId, index);

            synchronized (Main.class) {
                progressBars[index] = progressBar;
            }

            updateDisplay();

            try {
                Thread.sleep(threadNumber * 50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            for (int i = 0; i < CALCULATION_LENGTH; i++) {
                try {
                    Thread.sleep(STEP_DELAY_MS);
                    progressBar.updateProgress();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    progressBar.complete();
                    return;
                }
            }

            progressBar.complete();
        }
    }
}