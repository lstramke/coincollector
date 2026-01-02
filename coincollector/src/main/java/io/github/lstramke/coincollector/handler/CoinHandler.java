package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinDescription;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.Mint;
import io.github.lstramke.coincollector.model.DTOs.Requests.CoinActionRequest;
import io.github.lstramke.coincollector.model.DTOs.Responses.CoinResponse;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import io.github.lstramke.coincollector.services.EuroCoinStorageService;

public class CoinHandler implements HttpHandler {
    
    private final EuroCoinStorageService coinStorageService;
    private final EuroCoinCollectionStorageService collectionStorageService;
    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final ObjectMapper mapper;
    private final static Logger logger = LoggerFactory.getLogger(CoinHandler.class);
    private final static String PREFIX = "/api/coins";

    public CoinHandler(
        EuroCoinStorageService coinStorageService, 
        EuroCoinCollectionStorageService collectionStorageService, 
        EuroCoinCollectionGroupStorageService groupStorageService,
        ObjectMapper mapper
    ) {
        this.coinStorageService = coinStorageService;
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
        String coinId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var coin = this.coinStorageService.getById(coinId);

            if(handleIfNotOwnerViaCollection(exchange, coin.getCollectionId(), userId)) return;

            var response = CoinResponse.fromDomain(coin);
            String responseJson = mapper.writeValueAsString(response);
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
      
        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (JacksonException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }

    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        logger.info("handleCreate called");
        String userId = (String) exchange.getAttribute("userId");
        String body = new String(exchange.getRequestBody().readAllBytes());

        CoinActionRequest request;
        try {
            request = mapper.readValue(body, CoinActionRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Invalid request body\"}".getBytes());
            exchange.close();
            return;
        }

        try {
            if(handleIfNotOwnerViaCollection(exchange, request.collectionId(), userId)) return;

            var builder = new EuroCoinBuilder()
                .setYear(request.year())
                .setValue(CoinValue.fromCentValue(request.value()))
                .setMintCountry(CoinCountry.fromIsoCode(request.country()))
                .setDescription(request.description() != null ? new CoinDescription(request.description()) : null)
                .setCollectionId(request.collectionId());

            if (request.country().equals("DE")) {
                builder.setMint(Mint.fromMintMark(request.mint()));
            }

            var coin = builder.build();

            this.coinStorageService.save(coin);

            var response = CoinResponse.fromDomain(coin);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();
            
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Parent resource not found\"}".getBytes());
            exchange.close();
        } catch(EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinSaveException | JacksonException | IllegalArgumentException | IllegalStateException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        } catch (EuroCoinAlreadyExistsException e) {
            exchange.sendResponseHeaders(409, 0);
            exchange.getResponseBody().write("{\"error\":\"Coin already exists\"}".getBytes());
            exchange.close();
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        logger.info("handleUpdate called");
        String userId = (String) exchange.getAttribute("userId");
        String coinId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);
        String body = new String(exchange.getRequestBody().readAllBytes());

        CoinActionRequest request;
        try {
            request = mapper.readValue(body, CoinActionRequest.class);
        } catch (JacksonException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write("{\"error\":\"Invalid request body\"}".getBytes());
            exchange.close();
            return;
        }

        try {

            var coin = this.coinStorageService.getById(coinId);

            if(handleIfNotOwnerViaCollection(exchange, coin.getCollectionId(), userId)) return;

            if(!coin.getCollectionId().equals(request.collectionId())) {
                if(handleIfNotOwnerViaCollection(exchange, request.collectionId(), userId)) return;
            }

            var builder = new EuroCoinBuilder()
                .setYear(request.year())
                .setValue(CoinValue.fromCentValue(request.value()))
                .setMintCountry(CoinCountry.fromIsoCode(request.country()))
                .setDescription(request.description() != null ? new CoinDescription(request.description()) : null)
                .setCollectionId(request.collectionId());

            if (request.country().equals("DE")) {
                builder.setMint(Mint.fromMintMark(request.mint()));
            }

            var updatedCoin = builder.build();

            this.coinStorageService.delete(coinId);
            this.coinStorageService.save(updatedCoin);

            var response = CoinResponse.fromDomain(updatedCoin);
            String responseJson = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.getBytes().length);
            exchange.getResponseBody().write(responseJson.getBytes());
            exchange.close();

        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinSaveException | EuroCoinDeleteException | JacksonException | IllegalArgumentException | IllegalStateException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }

    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        logger.info("handleDelete called");
        String userId = (String) exchange.getAttribute("userId");
        String coinId = exchange.getRequestURI().getPath().substring(PREFIX.length() + 1);

        try {
            var coinToDelete = this.coinStorageService.getById(coinId);

            if(handleIfNotOwnerViaCollection(exchange, coinToDelete.getCollectionId(), userId)) return;

            this.coinStorageService.delete(coinId);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();

        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinDeleteException | EuroCoinCollectionCoinsLoadException e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write("{\"error\":\"Internal server error\"}".getBytes());
            exchange.close();
        }
    }

    private boolean handleIfNotOwnerViaCollection(
        HttpExchange exchange, 
        String collectionId, 
        String userId
    ) throws 
        IOException, 
        EuroCoinCollectionGetByIdException,
        EuroCoinCollectionNotFoundException,
        EuroCoinCollectionCoinsLoadException,
        EuroCoinCollectionGroupGetByIdException, 
        EuroCoinCollectionGroupNotFoundException 
    {
        var collection = this.collectionStorageService.getById(collectionId);
        var group = this.groupStorageService.getById(collection.getGroupId());
        if (!group.getOwnerId().equals(userId)) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"error\":\"Resource not found\"}".getBytes());
            exchange.close();
            return true;
        }
        return false;
    }
    
}
