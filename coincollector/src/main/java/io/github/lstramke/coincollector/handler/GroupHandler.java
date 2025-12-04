package io.github.lstramke.coincollector.handler;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Requests.CreateGroupRequest;
import io.github.lstramke.coincollector.model.DTOs.Requests.UpdateGroupRequest;
import io.github.lstramke.coincollector.model.DTOs.Responses.GroupMetadataResponse;
import io.github.lstramke.coincollector.model.DTOs.Responses.GroupsResponse;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class GroupHandler implements HttpHandler {

    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final ObjectMapper mapper = new JsonMapper();
    private final static Logger logger = LoggerFactory.getLogger(GroupHandler.class);
    private final static String PREFIX = "/api/groups";

    
    public GroupHandler(EuroCoinCollectionGroupStorageService groupStorageService) {
        this.groupStorageService = groupStorageService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "GET" -> {
                if (path.equals(PREFIX)) {
                    handleGetAll(exchange);
                } else if(isGroupIdPath(path)) {
                    handleGetWithId(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
            case "POST" -> handleCreate(exchange);
            case "PATCH" -> handleUpdate(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> {
               exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException{
        String userId = (String) exchange.getAttribute("userId");

        try {
            var allGroupsForUser = this.groupStorageService.getAllByUser(userId);
            var response = allGroupsForUser.stream()
                .map(GroupsResponse::fromDomain)
                .toList();
            
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            
        } catch (EuroCoinCollectionGroupGetAllException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }    
    
    private void handleCreate(HttpExchange exchange) throws IOException{
        String userId = (String) exchange.getAttribute("userId");
        String body = new String(exchange.getRequestBody().readAllBytes());

        try {
            var createGroupRequest = mapper.readValue(body, CreateGroupRequest.class);

            var requestedGroup = new EuroCoinCollectionGroup(createGroupRequest.name(), userId);
            this.groupStorageService.save(requestedGroup);

            var response = GroupsResponse.fromDomain(requestedGroup);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
        } catch (EuroCoinCollectionGroupSaveException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }

    private void handleGetWithId(HttpExchange exchange) throws IOException {
        String userId = (String) exchange.getAttribute("userId");
        String groupId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var group = this.groupStorageService.getById(groupId);
            
            if (!group.getOwnerId().equals(userId)) {
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
                exchange.getResponseBody().close();
                return;
            }
            
            var response = GroupsResponse.fromDomain(group);
            
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
        } catch (EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        String userId = (String) exchange.getAttribute("userId");
        String groupId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);
        String body = new String(exchange.getRequestBody().readAllBytes());

        try {
            var request = mapper.readValue(body, UpdateGroupRequest.class);
            
            var groupToUpdate = this.groupStorageService.getById(groupId);
            if (!groupToUpdate.getOwnerId().equals(userId)){
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
                exchange.getResponseBody().close();
                return;
            }

            groupToUpdate.setName(request.name());
            this.groupStorageService.updateMetadata(groupToUpdate);

            var response = new GroupMetadataResponse(groupToUpdate.getName());
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
        } catch (EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionGroupUpdateException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
        } finally {
            exchange.getResponseBody().close();
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException{

    }

    private boolean isGroupIdPath(String path) {
        if (path.startsWith(PREFIX + "/")) {
            String id = path.substring(PREFIX.length() + 1);
            try {
                UUID.fromString(id);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
    
}
