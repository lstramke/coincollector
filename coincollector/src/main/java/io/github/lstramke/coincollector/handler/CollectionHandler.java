package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.DTOs.Requests.CreateCollectionRequest;
import io.github.lstramke.coincollector.model.DTOs.Responses.CollectionResponse;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Handler for collection-related HTTP requests.
 * Manages CRUD operations for Euro coin collections within groups.
 * Validates ownership and authorization for all collection operations.
 */
public class CollectionHandler implements HttpHandler {

    private final EuroCoinCollectionStorageService collectionStorageService;
    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final ObjectMapper mapper;
    private final static Logger logger = LoggerFactory.getLogger(CollectionHandler.class);
    private final static String PREFIX = "/api/collections";

    /**
     * Constructs a new CollectionHandler with required dependencies.
     *
     * @param collectionStorageService the service for collection storage operations
     * @param groupStorageService the service for collection group storage operations
     * @param mapper the ObjectMapper for JSON serialization/deserialization
     */
    public CollectionHandler(EuroCoinCollectionStorageService collectionStorageService, EuroCoinCollectionGroupStorageService groupStorageService, ObjectMapper mapper) {
        this.collectionStorageService = collectionStorageService;
        this.groupStorageService = groupStorageService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Route called: {} {}", method, path);
        switch (method) {
            case "GET" -> handleGet(exchange);
            case "POST" -> handleCreate(exchange);
            case "PATCH" -> handleUpdate(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> {
               exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * Handles GET requests to retrieve a specific collection by ID.
     * Validates that the requesting user owns the collection through the group hierarchy.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        logger.info("handleGet called");
        String userId = (String) exchange.getAttribute("userId");
        String collectionId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var collection = this.collectionStorageService.getById(collectionId);
            
            if(handleIfNotOwnerViaGroup(exchange, collection.getGroupId(), userId)) return;
            
            var response = CollectionResponse.fromDomain(collection);
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
   
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (JacksonException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles POST requests to create a new collection.
     * Validates ownership of the target group before creation.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleCreate(HttpExchange exchange) throws IOException {
        logger.info("handleCreate called");
        String userId = (String) exchange.getAttribute("userId");
        String body = new String(exchange.getRequestBody().readAllBytes());

        CreateCollectionRequest request;
        try {
            request = mapper.readValue(body, CreateCollectionRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
            return;
        }

        try {
            if(handleIfNotOwnerViaGroup(exchange, request.groupId(), userId)) return;

            var requestedCollection = new EuroCoinCollection(request.name(), request.coins(), request.groupId());
            this.collectionStorageService.save(requestedCollection);

            var response = CollectionResponse.fromDomain(requestedCollection);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();

        } catch (JacksonException | EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Parent resource not found\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles PATCH requests to update an existing collection.
     * Validates ownership of both the source and target groups if the collection is moved.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleUpdate(HttpExchange exchange) throws IOException {
        logger.info("handleUpdate called");
        String userId = (String) exchange.getAttribute("userId");
        String collectionId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);
        String body = new String(exchange.getRequestBody().readAllBytes());

        CreateCollectionRequest request;
        try {
            request = mapper.readValue(body, CreateCollectionRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Request is not valid\"}".getBytes());
            exchange.close();
            return;
        }

        try {

            var collectionToUpdate = this.collectionStorageService.getById(collectionId);

            if(handleIfNotOwnerViaGroup(exchange, collectionToUpdate.getGroupId(), userId)) return;

            if(!collectionToUpdate.getGroupId().equals(request.groupId())) {
                if(handleIfNotOwnerViaGroup(exchange, request.groupId(), userId)) return;
            }

            collectionToUpdate.setName(request.name());
            collectionToUpdate.setGroupId(request.groupId());
            this.collectionStorageService.updateMetadata(collectionToUpdate);

            var response = CollectionResponse.fromDomain(collectionToUpdate);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();

        } catch (JacksonException | EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Parent resource not found\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Handles DELETE requests to remove a collection.
     * Validates ownership of the collection through the group hierarchy.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        logger.info("handleDelete called");
        String userId = (String) exchange.getAttribute("userId");
        String collectionId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var collectionToDelete = this.collectionStorageService.getById(collectionId);

            if(handleIfNotOwnerViaGroup(exchange, collectionToDelete.getGroupId(), userId)) return;

            this.collectionStorageService.delete(collectionId);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();

        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionDeleteException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    /**
     * Validates that the specified user owns the group.
     * Sends a 404 response and closes the exchange if the user is not the owner.
     *
     * @param exchange the HTTP exchange for sending error responses
     * @param groupId the ID of the group to check ownership for
     * @param userId the ID of the user to validate
     * @return true if the user is not the owner (response sent and exchange closed), false if the user is the owner
     * @throws IOException if an I/O error occurs
     * @throws EuroCoinCollectionGroupGetByIdException if retrieving the group fails
     * @throws EuroCoinCollectionGroupNotFoundException if the group is not found
     */
    private boolean handleIfNotOwnerViaGroup(
        HttpExchange exchange, 
        String groupId, 
        String userId
    ) throws 
        IOException, 
        EuroCoinCollectionGroupGetByIdException, 
        EuroCoinCollectionGroupNotFoundException 
    {
        var group = this.groupStorageService.getById(groupId);
        if (!group.getOwnerId().equals(userId)) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
            return true;
        }
        return false;
    }
}
