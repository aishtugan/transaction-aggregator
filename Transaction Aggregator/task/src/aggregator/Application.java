package aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
@EnableCaching
public class Application {

    private final AccountDataClient accountDataClient;

    public Application(AccountDataClient accountDataClient) {
        this.accountDataClient = accountDataClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public record AccountData(
            String id,
            String serverId,
            String account,
            String amount,
            String timestamp
    ) {}

    @GetMapping("/aggregate")
    public ResponseEntity<?> aggregate(@RequestParam String account) {

        RestTemplate restTemplate = new RestTemplate();
        String urlFirst = "http://localhost:8888";
        String urlSecond = "http://localhost:8889";
        String endpointName = "/transactions?account=" + account;
        String errorResponse1 = "";
        String errorResponse2 = "";
        Integer errorCode1 = 0;
        Integer errorCode2 = 0;
        int iterations = 5;

        for (int i = 1; i <= iterations; i++) {

            CompletableFuture<AccountDataClient.FetchResult> future1 = CompletableFuture.supplyAsync(
                    () -> accountDataClient.fetchAccountData(urlFirst + endpointName)
            );
            CompletableFuture<AccountDataClient.FetchResult> future2 = CompletableFuture.supplyAsync(
                    () -> accountDataClient.fetchAccountData(urlSecond + endpointName)
            );

            AccountDataClient.FetchResult fetchResult1 = future1.join();
            AccountDataClient.FetchResult fetchResult2 = future2.join();

            if (fetchResult1.statusCode() != null) {
                errorResponse1 = fetchResult1.error();
                errorCode1 = fetchResult1.statusCode();
                continue;
            }

            if (fetchResult2.statusCode() != null) {
                errorResponse2 = fetchResult2.error();
                errorCode2 = fetchResult2.statusCode();
                continue;
            }

            AccountData[] accountData1 = fetchResult1.data();
            AccountData[] accountData2 = fetchResult2.data();

            AccountData[] accountData = Stream.concat(
                    Arrays.stream(accountData1),
                    Arrays.stream(accountData2)
            ).toArray(AccountData[]::new);

            Arrays.sort(
                    accountData,
                    Comparator.comparing(AccountData::timestamp).reversed()
            );

            return ResponseEntity.ok(accountData);
        }

        return ResponseEntity.ok(new AccountData[0]);

    }
}
