package aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
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

    @GetMapping("/aggregate")
    public ResponseEntity<AccountData[]> aggregate(@RequestParam String account) {

        RestTemplate restTemplate = new RestTemplate();
        String urlFirst = "http://localhost:8888";
        String urlSecond = "http://localhost:8889";
        String endpointName = "/transactions?account=" + account;

        AccountData[] accountData1 = restTemplate.getForObject(urlFirst + endpointName, AccountData[].class);
        AccountData[] accountData2 = restTemplate.getForObject(urlSecond + endpointName, AccountData[].class);

        if (accountData1 == null) accountData1 = new AccountData[0];
        if (accountData2 == null) accountData2 = new AccountData[0];

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
}
