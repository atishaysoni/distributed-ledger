import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoadTest {

    static final String baseUrl = env("BASE_URL", "http://app:8080");
    static final int accountCount = Integer.parseInt(env("ACCOUNTS", "100"));
    static final int workers = Integer.parseInt(env("WORKERS", "50"));
    static final int durationSeconds = Integer.parseInt(env("DURATION_SECONDS", "20"));
    static final double initialBalance = Double.parseDouble(env("INITIAL_BALANCE", "10000"));
    static final double transferAmount = Double.parseDouble(env("TRANSFER_AMOUNT", "10"));

    static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static void main(String[] args) throws Exception {
        System.out.println("creating " + accountCount + " accounts...");
        List<String> accountIds = new ArrayList<>();
        for (int i = 0; i < accountCount; i++) {
            String body = "{\"owner\":\"loadtest-" + i + "\",\"initialBalance\":" + initialBalance + "}";
            HttpResponse<String> resp = post("/api/v1/accounts", body);
            accountIds.add(extractString(resp.body(), "id"));
        }

        BigDecimal startingTotal = BigDecimal.valueOf(initialBalance).multiply(BigDecimal.valueOf(accountCount));

        AtomicLong submitted = new AtomicLong();
        AtomicLong failed = new AtomicLong();
        List<List<Long>> latenciesPerWorker = new CopyOnWriteArrayList<>();
        AtomicBoolean running = new AtomicBoolean(true);

        ExecutorService pool = Executors.newFixedThreadPool(workers);

        System.out.println("running " + workers + " workers for " + durationSeconds + "s...");
        Instant start = Instant.now();

        for (int w = 0; w < workers; w++) {
            pool.submit(() -> {
                Random rnd = new Random();
                List<Long> local = new ArrayList<>();
                while (running.get()) {
                    int i = rnd.nextInt(accountCount);
                    int j = rnd.nextInt(accountCount);
                    if (i == j) continue;

                    String key = UUID.randomUUID().toString();
                    String body = "{\"fromAccountId\":\"" + accountIds.get(i) + "\","
                            + "\"toAccountId\":\"" + accountIds.get(j) + "\","
                            + "\"amount\":" + transferAmount + ","
                            + "\"idempotencyKey\":\"" + key + "\"}";

                    long t0 = System.nanoTime();
                    try {
                        HttpResponse<String> resp = post("/api/v1/transactions", body);
                        local.add((System.nanoTime() - t0) / 1000);
                        if (resp.statusCode() == 202) {
                            submitted.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                }
                latenciesPerWorker.add(local);
            });
        }

        Thread.sleep(durationSeconds * 1000L);
        running.set(false);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        Duration elapsed = Duration.between(start, Instant.now());

        System.out.println("draining consumer backlog...");
        BigDecimal previousTotal = null;
        BigDecimal currentTotal;
        int stableChecks = 0;

        while (stableChecks < 3) {
            currentTotal = BigDecimal.ZERO;
            for (String id : accountIds) {
                HttpResponse<String> resp = get("/api/v1/accounts/" + id);
                currentTotal = currentTotal.add(BigDecimal.valueOf(extractNumber(resp.body(), "balance")));
            }

            if (previousTotal != null && currentTotal.compareTo(previousTotal) == 0) {
                stableChecks++;
            } else {
                stableChecks = 0;
            }

            previousTotal = currentTotal;
            Thread.sleep(2000);
        }

        System.out.println("checking final balances...");
        BigDecimal endingTotal = BigDecimal.ZERO;
        int changedAccounts = 0;
        for (String id : accountIds) {
            HttpResponse<String> resp = get("/api/v1/accounts/" + id);
            BigDecimal balance = BigDecimal.valueOf(extractNumber(resp.body(), "balance"));
            endingTotal = endingTotal.add(balance);
            if (balance.subtract(BigDecimal.valueOf(initialBalance)).abs().compareTo(BigDecimal.valueOf(0.0001)) > 0) {
                changedAccounts++;
            }
    }

        List<Long> all = new ArrayList<>();
        for (List<Long> l : latenciesPerWorker) all.addAll(l);
        Collections.sort(all);
        long p50 = all.isEmpty() ? 0 : all.get(all.size() / 2);
        long p99 = all.isEmpty() ? 0 : all.get((int) (all.size() * 0.99));

        System.out.println();
        System.out.println("--- results ---");
        System.out.println("duration          " + elapsed.toSeconds() + "s");
        System.out.println("workers           " + workers);
        System.out.println("accounts          " + accountCount);
        System.out.println("submitted         " + submitted.get());
        System.out.println("failed            " + failed.get());
        System.out.printf("submission tps    %.0f%n", submitted.get() / (double) elapsed.toSeconds());
        System.out.println("p50 latency       " + (p50 / 1000.0) + "ms");
        System.out.println("p99 latency       " + (p99 / 1000.0) + "ms");
        System.out.println();
        System.out.println("--- consistency check ---");
        System.out.printf("starting total    %.2f%n", startingTotal.doubleValue());
        System.out.printf("ending total      %.2f%n", endingTotal.doubleValue());
        System.out.println("accounts changed  " + changedAccounts + " / " + accountCount);
        System.out.println(startingTotal.subtract(endingTotal).abs().compareTo(BigDecimal.valueOf(0.01)) < 0
            ? "BALANCE CONSERVED — no money created or destroyed"
            : "MISMATCH — investigate immediately");
    }

    static HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    static String extractString(String json, String field) {
        var m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static double extractNumber(String json, String field) {
        var m = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9.eE+-]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }

    static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v != null ? v : fallback;
    }
}