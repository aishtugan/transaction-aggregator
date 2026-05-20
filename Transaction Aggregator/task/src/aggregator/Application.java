package aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
public class Application {
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

    private record FetchResult(
            AccountData[] data,
            Integer statusCode,
            String error
    ) {}

    private FetchResult fetchAccountData(RestTemplate restTemplate, String url) {
        try {
            AccountData[] data = restTemplate.getForObject(url, AccountData[].class);
            return new FetchResult(data == null ? new AccountData[0] : data, null, null);
        } catch (HttpServerErrorException e) {
            return new FetchResult(new AccountData[0], e.getStatusCode().value(), e.getResponseBodyAsString().isBlank()
                                                                                    ? e.getStatusText()
                                                                                    : e.getResponseBodyAsString());
        }
    }
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

            FetchResult fetchResult1 = fetchAccountData(restTemplate, urlFirst + endpointName);
            FetchResult fetchResult2 = fetchAccountData(restTemplate, urlSecond + endpointName);

            if (fetchResult1.statusCode != null) {
                errorResponse1 = fetchResult1.error;
                errorCode1 = fetchResult1.statusCode;
                continue;
            }

            if (fetchResult2.statusCode != null) {
                errorResponse2 = fetchResult2.error;
                errorCode2 = fetchResult2.statusCode;
                continue;
            }

            AccountData[] accountData1 = fetchResult1.data;
            AccountData[] accountData2 = fetchResult2.data;

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

//        if (errorCode1 != 0) {
//            return ResponseEntity.status(errorCode1).body(errorResponse1);
//        } else if (errorCode2 != 0) {
//            return ResponseEntity.status(errorCode2).body(errorResponse2);
//        }
//
//        return ResponseEntity.status(503).body("Service unavailable");
    }
}
