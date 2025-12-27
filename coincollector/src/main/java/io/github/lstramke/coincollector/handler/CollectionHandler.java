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

public class CollectionHandler implements HttpHandler {

    private final EuroCoinCollectionStorageService collectionStorageService;
    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final ObjectMapper mapper;
    private final static Logger logger = LoggerFactory.getLogger(CollectionHandler.class);
    private final static String PREFIX = "/api/collections";

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
            exchange.getResponseBody().close();
   
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.getResponseBody().close();
        } catch (JacksonException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.getResponseBody().close();
        }
    }

    
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
            exchange.getResponseBody().close();
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
            exchange.getResponseBody().close();

        } catch (JacksonException | EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.getResponseBody().close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Parent resource not found\"}".getBytes());
            exchange.getResponseBody().close();
        }
    }
    
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
            exchange.getResponseBody().close();
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
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.getResponseBody().close();

        } catch (JacksonException | EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.getResponseBody().close();
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Parent resource not found\"}".getBytes());
            exchange.getResponseBody().close();
        }
    }
    
    private void handleDelete(HttpExchange exchange) throws IOException {
        logger.info("handleDelete called");
        String userId = (String) exchange.getAttribute("userId");
        String collectionId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var collectionToDelete = this.collectionStorageService.getById(collectionId);

            if(handleIfNotOwnerViaGroup(exchange, collectionToDelete.getGroupId(), userId)) return;

            this.collectionStorageService.delete(collectionId);
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();

        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.getResponseBody().close();
        } catch (EuroCoinCollectionDeleteException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.getResponseBody().close();
        }
    }

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
            exchange.getResponseBody().close();
            return true;
        }
        return false;
    }
}
