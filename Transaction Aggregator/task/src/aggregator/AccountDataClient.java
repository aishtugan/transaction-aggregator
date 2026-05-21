package aggregator;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
class AccountDataClient {
    private final RestTemplate restTemplate = new RestTemplate();

    public record FetchResult(
            Application.AccountData[] data,
            Integer statusCode,
            String error
    ) {}

    @Cacheable(
            value = "accountData",
            key = "#url",
            unless = "#result.statusCode() != null"
    )
    public FetchResult fetchAccountData(String url) {
        try {
            Application.AccountData[] data = restTemplate.getForObject(url, Application.AccountData[].class);
            return new FetchResult(data == null ? new Application.AccountData[0] : data, null, null);
        } catch (HttpServerErrorException e) {
            return new FetchResult(
                    new Application.AccountData[0],
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString().isBlank()
                            ? e.getStatusText()
                            : e.getResponseBodyAsString()
            );
        }
    }
}
