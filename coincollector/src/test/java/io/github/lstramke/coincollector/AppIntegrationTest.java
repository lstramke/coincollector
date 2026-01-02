package io.github.lstramke.coincollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppIntegrationTest {

    

    private record AppTestcase(
        String method,
        String route,
        String requestBody,
        String requestCookieHeader,
        Consumer<HttpResponse<String>> validator,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static final int PORT = 8081;
    private static final String DB_TESTFILE = "test-coincollector.db";
    private static final String BASE_URL = "http://localhost:" + PORT;

    private static String sessionId = "";
    private static String groupId = "";
    private static String groupToUpdateAndDeleteId = "";
    private static String collcetionId = "";
    private static String coinId = "";

    private Thread serverThread;

    @BeforeAll
    void startServer() throws InterruptedException {
        serverThread = new Thread(() -> {
            try {
                App.main(new String[]{DB_TESTFILE, String.valueOf(PORT)});
            } catch (Exception e) {
                fail("unexpected exception during server start up");
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(2000);
    }

    @AfterAll
    void stopServer(){
        App.stopServer();
        Path dbPath = Path.of(DB_TESTFILE);
        try {
            Files.deleteIfExists(dbPath);
        } catch (Exception e) {
            System.err.println("WARNING: Test database could not be deleted: " + dbPath);
        }
    }

    private static Stream<Supplier<AppTestcase>> appTestcases() {
        return Stream.of(
            () -> new AppTestcase(
                "GET", 
                "/", 
                "", 
                null,
                response -> {
                    assertEquals(200, response.statusCode());
                    assertTrue(response.body().contains("<html"));
                },
                ""
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/login", 
                "{\"username\":\"testuser\"}", 
                null,
                response -> {
                    assertEquals(400, response.statusCode());
                    assertTrue(response.body().contains("error"), "Login with empty Db should return error");
                },
                "Login fails when user does not exist"
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/registration", 
                "{\"username\":\"testuser\"}", 
                null,
                response -> {
                    assertEquals(201, response.statusCode());
                    var setCookieOpt = response.headers().firstValue("Set-Cookie");
                    assertTrue(setCookieOpt.isPresent(), "Set-Cookie header should be present");
                    var setCookie = setCookieOpt.get();
                    assertTrue(setCookie.contains("sessionId="), "Set-Cookie should contain sessionId");
                    sessionId = setCookie.split("sessionId=")[1].split(";")[0];
                },
                "Registration returns sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/groups",
                "{\"name\":\"TestGroup\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Create group without session cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/groups",
                "{\"name\":\"TestGroup\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(201, response.statusCode());
                    assertTrue(response.body().contains("\"id\""));
                    int idStart = response.body().indexOf("\"id\":\"") + 6;
                    int idEnd = response.body().indexOf("\"", idStart);
                    groupId = response.body().substring(idStart, idEnd);
    
                },
                "Create group with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/groups", 
                null, 
                null, 
                response -> {
                    assertEquals(302, response.statusCode());
                }, 
                "Get groups without sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/groups", 
                null, 
                "sessionId=" + sessionId, 
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
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
                "/api/groups/"+groupId, 
                null, 
                "sessionId=" + sessionId, 
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
                    assertEquals(1, body.split("\\{").length - 1);
                    assertTrue(body.contains("\"id\":\"" + groupId + "\""));
                    assertTrue(body.contains("\"name\":\"TestGroup\""));
                    assertTrue(body.contains("\"collections\":[]"));
                }, 
                "Get specific group with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/groups/"+groupId, 
                null, 
                null, 
                response -> {
                    assertEquals(302, response.statusCode());
                }, 
                "Get specific group without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/groups",
                "{\"name\":\"TestGroupToUpdateAndDelete\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(201, response.statusCode());
                    assertTrue(response.body().contains("\"id\""));
                    int idStart = response.body().indexOf("\"id\":\"") + 6;
                    int idEnd = response.body().indexOf("\"", idStart);
                    groupToUpdateAndDeleteId = response.body().substring(idStart, idEnd);
    
                },
                "Create a second group for other endpoints"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/groups/" + groupToUpdateAndDeleteId,
                "{\"name\":\"UpdateGroupName\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(200, response.statusCode());
                    assertTrue(response.body().contains("\"name\":\"UpdateGroupName\""));
                },
                "Update second group"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/groups/" + groupToUpdateAndDeleteId,
                "{\"name\":\"UpdatedGroupName\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Update second group without session cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/groups/" + groupToUpdateAndDeleteId,
                null,
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(204, response.statusCode());
                },
                "Delete second group"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/groups/" + groupToUpdateAndDeleteId,
                null,
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Delete second group without sesssion cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/collections",
                "{\"name\":\"TestCollection\",\"groupId\":\"" + groupId + "\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Create collection without session cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/collections",
                "{\"name\":\"TestCollection\",\"groupId\":\"" + groupId + "\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(201, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"name\":\"TestCollection\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                    int idStart = response.body().indexOf("\"id\":\"") + 6;
                    int idEnd = response.body().indexOf("\"", idStart);
                    collcetionId = response.body().substring(idStart, idEnd);
                },
                "Create collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/collections/" + collcetionId,
                null,
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\":\"" + collcetionId + "\""));
                    assertTrue(body.contains("\"name\":\"TestCollection\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                    assertTrue(body.contains("\"coins\":[]"));
                },
                "Get collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/collections/" + collcetionId,
                null,
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Get collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/collections/" + collcetionId,
                "{\"name\":\"TestCollectionNewName\",\"groupId\":\"" + groupId + "\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\":\"" + collcetionId + "\""));
                    assertTrue(body.contains("\"name\":\"TestCollectionNewName\""));
                    assertTrue(body.contains("\"groupId\":\"" + groupId + "\""));
                },
                "Update collection with sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/collections/" + collcetionId,
                "{\"name\":\"TestCollectionNewName\",\"groupId\":\"" + groupId + "\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Update collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/coins",
                "{\"year\":2024,\"value\":200,\"country\":\"DE\",\"collectionId\":\"" + collcetionId + "\",\"mint\":\"A\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Add coin to collection without sessionId cookie"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/coins",
                "{\"year\":2024,\"value\":200,\"country\":\"DE\",\"collectionId\":\"" + collcetionId + "\",\"mint\":\"A\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(201, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":200"));
                    assertTrue(body.contains("\"country\":\"DE\""), body);
                    assertTrue(body.contains("\"collectionId\":\"" + collcetionId + "\""));
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
                "/api/coins/" + coinId,
                null,
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\":\"" + coinId + "\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":200"));
                    assertTrue(body.contains("\"country\":\"DE\""));
                    assertTrue(body.contains("\"collectionId\":\"" + collcetionId + "\""));
                    assertTrue(body.contains("\"mint\":\"A\""));
                    assertTrue(body.contains("\"description\":"));
                },
                "Get coin with sessionId cookie"
            ),
            () -> new AppTestcase(
                "GET",
                "/api/coins/" + coinId,
                null,
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Get coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "PATCH",
                "/api/coins/" + coinId,
                "{\"year\":2024,\"value\":5,\"country\":\"DE\",\"collectionId\":\"" + collcetionId + "\",\"mint\":\"A\"}",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
                    assertTrue(body.contains("\"id\""));
                    assertTrue(body.contains("\"year\":2024"));
                    assertTrue(body.contains("\"value\":5"));
                    assertTrue(body.contains("\"country\":\"DE\""), body);
                    assertTrue(body.contains("\"collectionId\":\"" + collcetionId + "\""));
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
                "/api/coins/" + coinId,
                "{\"year\":2024,\"value\":5,\"country\":\"DE\",\"collectionId\":\"" + collcetionId + "\",\"mint\":\"A\"}",
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Update coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/coins/" + coinId,
                null,
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Delete coin without sessionId cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/coins/" + coinId,
                null,
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(204, response.statusCode());
                },
                "Delete coin"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/collections/" + collcetionId,
                null,
                null,
                response -> {
                    assertEquals(302, response.statusCode());
                },
                "Delete collection without sessionid cookie"
            ),
            () -> new AppTestcase(
                "DELETE",
                "/api/collections/" + collcetionId,
                null,
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(204, response.statusCode());
                },
                "Delete collection"
            ),
            () -> new AppTestcase(
                "POST",
                "/api/logout",
                "",
                "sessionId=" + sessionId,
                response -> {
                    assertEquals(204, response.statusCode());
                    var setCookieOpt = response.headers().firstValue("Set-Cookie");
                    assertTrue(setCookieOpt.isPresent(), "Set-Cookie header should be present after logout");
                    var setCookie = setCookieOpt.get();
                    assertTrue(setCookie.contains("sessionId=;"), "SessionId should be cleared after logout");
                },
                "Logout and check session cookie is cleared"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/groups", 
                null, 
                "sessionId=" + sessionId, 
                response -> {
                    assertEquals(302, response.statusCode());
                }, 
                "Get groups with sessionId cookie after session logout"
            ),
            () -> new AppTestcase(
                "POST", 
                "/api/login", 
                "{\"username\":\"testuser\"}", 
                null,
                response -> {
                    assertEquals(200, response.statusCode());
                    var setCookieOpt = response.headers().firstValue("Set-Cookie");
                    assertTrue(setCookieOpt.isPresent(), "Set-Cookie header should be present");
                    var setCookie = setCookieOpt.get();
                    assertTrue(setCookie.contains("sessionId="), "Set-Cookie should contain sessionId");
                    sessionId = setCookie.split("sessionId=")[1].split(";")[0];
                },
                "Login when user exists"
            ),
            () -> new AppTestcase(
                "GET", 
                "/api/groups", 
                null, 
                "sessionId=" + sessionId, 
                response -> {
                    assertEquals(200, response.statusCode());
                    var body = response.body();
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
    void testApp(Supplier<AppTestcase> testcaseSupplier) throws IOException, InterruptedException {
        var testcase = testcaseSupplier.get();
        var client = HttpClient.newHttpClient();

        var builder = HttpRequest.newBuilder().uri(URI.create(BASE_URL + testcase.route));

        switch (testcase.method) {
            case "POST" -> {
                builder.POST(HttpRequest.BodyPublishers.ofString(testcase.requestBody))
                    .header("Content-Type", "application/json");
            }
            case "GET" -> {
                builder.GET();
            }
            case "PATCH" -> {
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(testcase.requestBody))
                .header("Content-Type", "application/json");
            }
            case "DELETE" -> {
                builder.DELETE();
            }   
            default -> {
                fail();
            }
        }

        if (testcase.requestCookieHeader != null) {
            builder.header("Cookie", testcase.requestCookieHeader);
        }

        var request = builder.build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        testcase.validator().accept(response);
    }

}
