import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.mocks.web.response.HttpResponse;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.util.UUID;

public class ApplicationTests extends SpringTest {
    private final String serverId = UUID.randomUUID().toString();
    private final DataServer dataServer = new DataServer(serverId);

    CheckResult testPing() {
        CheckResult result;
        var response = get("/aggregate").send();

        System.out.println(getRequestDetails(response));

        if (response.getStatusCode() == 200) {
            var expected = "Pong from " + serverId;
            var actual = response.getContent();
            if (expected.equals(actual)) {
                result = CheckResult.correct();
            } else {
                result = CheckResult.wrong(
                        "Response doesn't contain the expected body.\nExpected: %s\n  Actual: %s\n"
                                .formatted(expected, actual)
                );
            }
        } else {
            result = CheckResult.wrong("Expected response status code 200 but got " + response.getStatusCode());
        }

        dataServer.stop();
        return result;
    }

    private String getRequestDetails(HttpResponse response) {
        var uri = response.getRequest().getUri();
        var method = response.getRequest().getMethod();
        return "\nRequest: %s %s".formatted(method, uri);
    }

    @DynamicTest
    DynamicTesting[] dt = new DynamicTesting[] {
            this::testPing
    };
}
