package io.github.lstramke.coincollector.handler;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
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

/**
 * Handler for collection group-related HTTP requests.
 * Manages CRUD operations for Euro coin collection groups.
 * Validates ownership and authorization for all group operations.
 */
public class GroupHandler implements HttpHandler {

    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final ObjectMapper mapper;
    private final static Logger logger = LoggerFactory.getLogger(GroupHandler.class);
    private final static String PREFIX = "/api/groups";

    /**
     * Constructs a new GroupHandler with required dependencies.
     *
     * @param groupStorageService the service for collection group storage operations
     * @param mapper the ObjectMapper for JSON serialization/deserialization
     */
    public GroupHandler(EuroCoinCollectionGroupStorageService groupStorageService, ObjectMapper mapper) {
        this.groupStorageService = groupStorageService;
        this.mapper = mapper;
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
               exchange.close();
            }
        }
    }

    /**
     * Handles GET requests to retrieve all groups belonging to the authenticated user.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleGetAll(HttpExchange exchange) throws IOException {
        logger.info("handleGetAll called");
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
            exchange.close();
            
        } catch (EuroCoinCollectionGroupGetAllException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }    

    /**
     * Handles POST requests to create a new collection group.
     * The authenticated user becomes the owner of the created group.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleCreate(HttpExchange exchange) throws IOException {
        logger.info("handleCreate called");
        String userId = (String) exchange.getAttribute("userId");
        String body = new String(exchange.getRequestBody().readAllBytes());

        CreateGroupRequest createGroupRequest;
        try {
            createGroupRequest = mapper.readValue(body, CreateGroupRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
            return;
        }

        try {
            var requestedGroup = new EuroCoinCollectionGroup(createGroupRequest.name(), userId);
            this.groupStorageService.save(requestedGroup);

            var response = GroupsResponse.fromDomain(requestedGroup);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
            
        } catch (EuroCoinCollectionGroupSaveException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles GET requests to retrieve a specific group by ID.
     * Validates that the requesting user owns the group.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleGetWithId(HttpExchange exchange) throws IOException {
        logger.info("handleGetWithId called");
        String userId = (String) exchange.getAttribute("userId");
        String groupId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var group = this.groupStorageService.getById(groupId);
            
            if(handleIfNotOwner(exchange, group, userId)) return;
            
            var response = GroupsResponse.fromDomain(group);
            
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles PATCH requests to update an existing group's metadata.
     * Validates that the requesting user owns the group before updating.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleUpdate(HttpExchange exchange) throws IOException {
        logger.info("handleUpdate called");
        String userId = (String) exchange.getAttribute("userId");
        String groupId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);
        String body = new String(exchange.getRequestBody().readAllBytes());

        UpdateGroupRequest request;
        try {
            request = mapper.readValue(body, UpdateGroupRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
            return;
        }

        try {
            var groupToUpdate = this.groupStorageService.getById(groupId);
            if(handleIfNotOwner(exchange, groupToUpdate, userId)) return;

            groupToUpdate.setName(request.name());
            this.groupStorageService.updateMetadata(groupToUpdate);

            var response = new GroupMetadataResponse(groupToUpdate.getName());
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
            
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (JacksonException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionGroupUpdateException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles DELETE requests to remove a group.
     * Validates that the requesting user owns the group before deletion.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        logger.info("handleDelete called");
        String userId = (String) exchange.getAttribute("userId");
        String groupId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var groupToDelete = this.groupStorageService.getById(groupId);
            if(handleIfNotOwner(exchange, groupToDelete, userId)) return;

            this.groupStorageService.delete(groupId);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        } catch (EuroCoinCollectionGroupDeleteException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Checks if the given path represents a valid group ID path.
     * Validates that the path follows the pattern /api/groups/{uuid}.
     *
     * @param path the request path to validate
     * @return true if the path contains a valid UUID after the prefix, false otherwise
     */
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

    /**
     * Validates that the specified user owns the group.
     * Sends a 404 response and closes the exchange if the user is not the owner.
     *
     * @param exchange the HTTP exchange for sending error responses
     * @param group the group to check ownership for
     * @param userId the ID of the user to validate
     * @return true if the user is not the owner (response sent and exchange closed), false if the user is the owner
     * @throws IOException if an I/O error occurs
     */
    private boolean handleIfNotOwner(
        HttpExchange exchange, 
        EuroCoinCollectionGroup group, 
        String userId
    ) throws IOException {
        if (!group.getOwnerId().equals(userId)) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
            return true;
        }
        return false;
    }
    
}
