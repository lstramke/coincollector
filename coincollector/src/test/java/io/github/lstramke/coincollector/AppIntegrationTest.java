package io.github.lstramke.coincollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import io.github.lstramke.coincollector.configuration.SqliteInitializer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestRestTemplate
public class AppIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    SqliteInitializer sqliteInitializer;

    private record AppTestcase(
        String method,
        String route,
        String requestBody,
        Boolean requestCookie,
        Consumer<ResponseEntity<String>> validator,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static final String DB_TESTFILE = "test.db";

    private String sessionId = "";
    private String groupId = "";
    private String groupToUpdateAndDeleteId = "";
    private String collectionId = "";
    private String coinId = "";
    private String storedCookie = null;

    @BeforeAll
    void initDb() throws Exception {
        sqliteInitializer.init();
    }

    @AfterAll
    void cleanup() {
        Path dbPath = Path.of(DB_TESTFILE);
        try {
            Files.deleteIfExists(dbPath);
        } catch (Exception e) {
            System.err.println("WARNING: Test database could not be deleted: " + dbPath);
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private ResponseEntity<String> sendRequest(String method, String route, String requestBody, Boolean requestCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
        if (requestCookie) {
            headers.set(HttpHeaders.COOKIE, storedCookie);
        }

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(
            getBaseUrl() + route,
            HttpMethod.valueOf(method),
            entity,
            String.class
        );
    }

    private Stream<Supplier<AppTestcase>> appTestcases() {
        return Stream.of(
            () -> new AppTestcase(
                "GET", 
                "/", 
                "", 
                false,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertTrue(response.getBody().contains("<html"));
                },
                ""
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/v1/login", 
                "{\"username\":\"testuser\"}", 
                false,
                response -> {
                    assertEquals(400, response.getStatusCode().value());
                    assertTrue(response.getBody().contains("error"), "Login with empty Db should return error");
                },
                "Login fails when user does not exist"
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/v1/registration", 
                "{\"username\":\"testuser\"}", 
                false,
                response -> {
                    assertEquals(201, response.getStatusCode().value());
                    var setCookie = response.getHeaders().getFirst("Set-Cookie");
                    assertTrue(setCookie != null, "Set-Cookie header should be present");
                    assertTrue(setCookie.contains("sessionId="), "Set-Cookie should contain sessionId");
                    sessionId = setCookie.split("sessionId=")[1].split(";")[0];
                    storedCookie = "sessionId=" + sessionId;
                },
                "Registration returns sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/groups",
                "{\"name\":\"TestGroup\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Create group without session cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/groups",
                "{\"name\":\"TestGroup\"}",
                true,
                response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(response.getBody().contains("\"id\""));
                    int idStart = response.getBody().indexOf("\"id\":\"" ) + 6;
                    int idEnd = response.getBody().indexOf("\"", idStart);
                    groupId = response.getBody().substring(idStart, idEnd);
    
                },
                "Create group with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups", 
                null, 
                false, 
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                }, 
                "Get groups without sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups", 
                null, 
                true, 
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.trim().startsWith("["));
                    assertTrue(body.trim().endsWith("]"));
                    int startObj = body.indexOf("{");
                    int endObj = body.indexOf("}");
                    String groupJson = body.substring(startObj, endObj + 1);
                    assertEquals(1, body.split("\\{").length - 1);
                    assertTrue(groupJson.contains("\"id\":\"" + groupId + "\""));
                    assertTrue(groupJson.contains("\"name\":\"TestGroup\""));
                    assertTrue(groupJson.contains("\"collections\":[]"));
                }, 
                "Get groups with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups/"+groupId, 
                null, 
                true, 
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertEquals(1, body.split("\\{").length - 1);
                    assertTrue(body.contains("\"id\":\"" + groupId + "\""));
                    assertTrue(body.contains("\"name\":\"TestGroup\""));
                    assertTrue(body.contains("\"collections\":[]"));
                }, 
                "Get specific group with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups/"+groupId, 
                null, 
                false, 
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                }, 
                "Get specific group without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/groups",
                "{\"name\":\"TestGroupToUpdateAndDelete\"}",
                true,
                response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertTrue(response.getBody().contains("\"id\""));
                    int idStart = response.getBody().indexOf("\"id\":\"" ) + 6;
                    int idEnd = response.getBody().indexOf("\"", idStart);
                    groupToUpdateAndDeleteId = response.getBody().substring(idStart, idEnd);
    
                },
                "Create a second group for other endpoints"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/groups/" + groupToUpdateAndDeleteId,
                "{\"name\":\"UpdateGroupName\"}",
                true,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertTrue(response.getBody().contains("\"name\":\"UpdateGroupName\""));
                },
                "Update second group"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/groups/" + groupToUpdateAndDeleteId,
                "{\"name\":\"UpdatedGroupName\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Update second group without session cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/groups/" + groupToUpdateAndDeleteId,
                null,
                true,
                response -> {
                    assertEquals(204, response.getStatusCode().value());
                },
                "Delete second group"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/groups/" + groupToUpdateAndDeleteId,
                null,
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Delete second group without sesssion cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/collections",
                "{\"name\":\"TestCollection\",\"groupId\":\"" + groupId + "\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Create collection without session cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/collections",
                "{\"name\":\"TestCollection\",\"groupId\":\"" + groupId + "\"}",
                true,
                response -> {
                    assertEquals(201, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"name\":\"TestCollection\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                    int idStart = response.getBody().indexOf("\"id\":\"" ) + 6;
                    int idEnd = response.getBody().indexOf("\"", idStart);
                    collectionId = response.getBody().substring(idStart, idEnd);
                },
                "Create collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/v1/collections/" + collectionId,
                null,
                true,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\":\"" + collectionId + "\""));
                    assertTrue(body.contains("\"name\":\"TestCollection\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                    assertTrue(body.contains("\"coins\":[]"));
                },
                "Get collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/v1/collections/" + collectionId,
                null,
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Get collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/collections/" + collectionId,
                "{\"name\":\"TestCollectionNewName\",\"groupId\":\"" + groupId + "\"}",
                true,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\":\"" + collectionId + "\""));
                    assertTrue(body.contains("\"name\":\"TestCollectionNewName\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                },
                "Update collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/collections/" + collectionId,
                "{\"name\":\"TestCollectionNewName\",\"groupId\":\"" + groupId + "\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Update collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/coins",
                "{\"year\":2024,\"value\":200,\"country\":\"DE\",\"collectionId\":\"" + collectionId + "\",\"mint\":\"A\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Add coin to collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/coins",
                "{\"year\":2024,\"value\":200,\"country\":\"DE\",\"collectionId\":\"" + collectionId + "\",\"mint\":\"A\"}",
                true,
                response -> {
                    assertEquals(201, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":200"));
                    assertTrue(body.contains("\"country\":\"DE\""), body);
                    assertTrue(body.contains("\"collectionId\":\"" + collectionId + "\""));
                    assertTrue(body.contains("\"mint\":\"A\""));
                    assertTrue(body.contains("\"description\":"));
                    int idStart = body.indexOf("\"id\":\"") + 6;
                    int idEnd = body.indexOf("\"", idStart);
                    coinId = body.substring(idStart, idEnd);
                },
                "Add coin to collection"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/v1/coins/" + coinId,
                null,
                true,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\":\"" + coinId + "\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":200"));
                    assertTrue(body.contains("\"country\":\"DE\""));
                    assertTrue(body.contains("\"collectionId\":\"" + collectionId + "\""));
                    assertTrue(body.contains("\"mint\":\"A\""));
                    assertTrue(body.contains("\"description\":"));
                },
                "Get coin with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/v1/coins/" + coinId,
                null,
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Get coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/coins/" + coinId,
                "{\"year\":2024,\"value\":5,\"country\":\"DE\",\"collectionId\":\"" + collectionId + "\",\"mint\":\"A\"}",
                true,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":5"));
                    assertTrue(body.contains("\"country\":\"DE\""), body);
                    assertTrue(body.contains("\"collectionId\":\"" + collectionId + "\""));
                    assertTrue(body.contains("\"mint\":\"A\""));
                    assertTrue(body.contains("\"description\":"));
                    int idStart = body.indexOf("\"id\":\"") + 6;
                    int idEnd = body.indexOf("\"", idStart);
                    coinId = body.substring(idStart, idEnd);
                },
                "Update coin"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/v1/coins/" + coinId,
                "{\"year\":2024,\"value\":5,\"country\":\"DE\",\"collectionId\":\"" + collectionId + "\",\"mint\":\"A\"}",
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Update coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/coins/" + coinId,
                null,
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Delete coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/coins/" + coinId,
                null,
                true,
                response -> {
                    assertEquals(204, response.getStatusCode().value());
                },
                "Delete coin"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/collections/" + collectionId,
                null,
                false,
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                },
                "Delete collection without sessionid cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/v1/collections/" + collectionId,
                null,
                true,
                response -> {
                    assertEquals(204, response.getStatusCode().value());
                },
                "Delete collection"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/v1/logout",
                "",
                true,
                response -> {
                    assertEquals(204, response.getStatusCode().value());
                    var setCookie = response.getHeaders().getFirst("Set-Cookie");
                    assertTrue(setCookie != null, "Set-Cookie header should be present after logout");
                    assertTrue(setCookie.contains("sessionId=;"), "SessionId should be cleared after logout");
                },
                "Logout and check session cookie is cleared"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups", 
                null, 
                true, 
                response -> {
                    assertEquals(401, response.getStatusCode().value());
                }, 
                "Get groups with sessionId cookie after session logout"
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/v1/login", 
                "{\"username\":\"testuser\"}", 
                false,
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var setCookie = response.getHeaders().getFirst("Set-Cookie");
                    assertTrue(setCookie != null, "Set-Cookie header should be present");
                    assertTrue(setCookie.contains("sessionId="), "Set-Cookie should contain sessionId");
                    sessionId = setCookie.split("sessionId=")[1].split(";")[0];
                    storedCookie = "sessionId=" + sessionId;
                },
                "Login when user exists"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/v1/groups", 
                null, 
                true, 
                response -> {
                    assertEquals(200, response.getStatusCode().value());
                    var body = response.getBody();
                    assertTrue(body.trim().startsWith("["));
                    assertTrue(body.trim().endsWith("]"));
                    int startObj = body.indexOf("{");
                    int endObj = body.indexOf("}");
                    String groupJson = body.substring(startObj, endObj + 1);
                    assertEquals(1, body.split("\\{").length - 1);
                    assertTrue(groupJson.contains("\"id\":\"" + groupId + "\""));
                    assertTrue(groupJson.contains("\"name\":\"TestGroup\""));
                    assertTrue(groupJson.contains("\"collections\":[]"));
                }, 
                "Get groups with sessionId cookie"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("appTestcases")
    void testApp(Supplier<AppTestcase> testcaseSupplier) {
        var testcase = testcaseSupplier.get();
        var response = sendRequest(testcase.method, testcase.route, testcase.requestBody, testcase.requestCookie);
        testcase.validator().accept(response);
    }

}
