package io.github.lstramke.coincollector.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
import io.github.lstramke.coincollector.model.DTOs.Requests.CoinActionRequest;
import io.github.lstramke.coincollector.model.DTOs.Responses.CoinResponse;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.Mint;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import io.github.lstramke.coincollector.services.EuroCoinStorageService;

/**
 * Handles coin-related REST endpoints.
 */
@RestController
@RequestMapping("/api/v1/coins")
public class CoinHandler {

    private static final Logger logger = LoggerFactory.getLogger(CoinHandler.class);

    private final EuroCoinStorageService coinStorageService;
    private final EuroCoinCollectionStorageService collectionStorageService;
    private final EuroCoinCollectionGroupStorageService groupStorageService;

    public CoinHandler(
        EuroCoinStorageService coinStorageService,
        EuroCoinCollectionStorageService collectionStorageService,
        EuroCoinCollectionGroupStorageService groupStorageService
    ) {
        this.coinStorageService = coinStorageService;
        this.collectionStorageService = collectionStorageService;
        this.groupStorageService = groupStorageService;
    }

    /**
     * Returns a single coin if the authenticated user owns it.
     */
    @GetMapping("/{coinId}")
    public ResponseEntity<CoinResponse> getCoin(@PathVariable String coinId, Authentication authentication) {
        logger.info("Coin read requested: id={}", coinId);
        String userId = requireUserId(authentication);

        try {
            var coin = coinStorageService.getById(coinId);
            assertOwnerViaCollection(coin.getCollectionId(), userId);
            return ResponseEntity.ok(CoinResponse.fromDomain(coin));
        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Creates a new coin for the authenticated user's collection.
     */
    @PostMapping
    public ResponseEntity<CoinResponse> createCoin(@RequestBody CoinActionRequest request, Authentication authentication) {
        logger.info("Coin create requested: collectionId={}", request.collectionId());
        String userId = requireUserId(authentication);

        try {
            assertOwnerViaCollection(request.collectionId(), userId);

            var builder = new EuroCoinBuilder()
                .setYear(request.year())
                .setValue(CoinValue.fromCentValue(request.value()))
                .setMintCountry(CoinCountry.fromIsoCode(request.country()))
                .setDescription(request.description() != null ? new CoinDescription(request.description()) : null)
                .setCollectionId(request.collectionId());

            if ("DE".equals(request.country())) {
                builder.setMint(Mint.fromMintMark(request.mint()));
            }

            var coin = builder.build();
            coinStorageService.save(coin);

            logger.info("Coin created: collectionId={}", request.collectionId());

            return ResponseEntity.status(HttpStatus.CREATED).body(CoinResponse.fromDomain(coin));
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent resource not found", e);
        } catch (EuroCoinAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coin already exists", e);
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinSaveException | IllegalArgumentException | IllegalStateException | EuroCoinCollectionCoinsLoadException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Updates an existing coin.
     */
    @PatchMapping("/{coinId}")
    public ResponseEntity<CoinResponse> updateCoin(
        @PathVariable String coinId,
        @RequestBody CoinActionRequest request,
        Authentication authentication
    ) {
        logger.info("Coin update requested: id={}", coinId);
        String userId = requireUserId(authentication);

        try {
            var coin = coinStorageService.getById(coinId);
            assertOwnerViaCollection(coin.getCollectionId(), userId);

            if (!coin.getCollectionId().equals(request.collectionId())) {
                assertOwnerViaCollection(request.collectionId(), userId);
            }

            var builder = new EuroCoinBuilder()
                .setYear(request.year())
                .setValue(CoinValue.fromCentValue(request.value()))
                .setMintCountry(CoinCountry.fromIsoCode(request.country()))
                .setDescription(request.description() != null ? new CoinDescription(request.description()) : null)
                .setCollectionId(request.collectionId());

            if ("DE".equals(request.country())) {
                builder.setMint(Mint.fromMintMark(request.mint()));
            }

            var updatedCoin = builder.build();
            coinStorageService.delete(coinId);
            coinStorageService.save(updatedCoin);

            logger.info("Coin updated: id={}", coinId);

            return ResponseEntity.ok(CoinResponse.fromDomain(updatedCoin));
        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinSaveException | EuroCoinDeleteException | IllegalArgumentException | IllegalStateException | EuroCoinCollectionCoinsLoadException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Deletes a coin if the authenticated user owns it.
     */
    @DeleteMapping("/{coinId}")
    public ResponseEntity<Void> deleteCoin(@PathVariable String coinId, Authentication authentication) {
        logger.info("Coin delete requested: id={}", coinId);
        String userId = requireUserId(authentication);

        try {
            var coinToDelete = coinStorageService.getById(coinId);
            assertOwnerViaCollection(coinToDelete.getCollectionId(), userId);

            coinStorageService.delete(coinId);

            logger.info("Coin deleted: id={}", coinId);
            return ResponseEntity.noContent().build();
        } catch (EuroCoinNotFoundException | EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinDeleteException | EuroCoinCollectionCoinsLoadException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Checks that the authenticated user owns the given collection.
     */
    private void assertOwnerViaCollection(String collectionId, String userId) {
        var collection = collectionStorageService.getById(collectionId);
        var group = groupStorageService.getById(collection.getGroupId());

        if (!group.getOwnerId().equals(userId)) {
            logger.warn("Coin access denied: collectionId={}, userId={}", collectionId, userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    /**
     * Extracts the user id from the Spring Security authentication.
     */
    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.debug("UserUd required but not existent");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getPrincipal().toString();
    }
}
