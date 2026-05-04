package io.github.lstramke.coincollector.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Handler for collection-related HTTP requests.
 * Manages CRUD operations for Euro coin collections within groups.
 * Validates ownership and authorization for all collection operations.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class CollectionHandler {

    private final EuroCoinCollectionStorageService collectionStorageService;
    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final static Logger logger = LoggerFactory.getLogger(CollectionHandler.class);

    /**
     * Constructs a new CollectionHandler with required dependencies.
    *
    * @param collectionStorageService the service for collection storage operations
    * @param groupStorageService the service for collection group storage operations
    */
    @Autowired
    public CollectionHandler(EuroCoinCollectionStorageService collectionStorageService, EuroCoinCollectionGroupStorageService groupStorageService) {
        this.collectionStorageService = collectionStorageService;
        this.groupStorageService = groupStorageService;
    }

    /**
     * Handles GET requests to retrieve a specific collection by ID.
     * Validates that the requesting user owns the collection through the group hierarchy.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    @GetMapping("/{collectionId}")
    private ResponseEntity<CollectionResponse> getCollection(@PathVariable String collectionId, Authentication authentication) {
        logger.info("Collection read requested: id={}", collectionId);
        String userId = requireUserId(authentication);

        try {
            var collection = collectionStorageService.getById(collectionId);
            assertOwnerViaGroup(collectionId, userId);
            return ResponseEntity.ok(CollectionResponse.fromDomain(collection));
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Handles POST requests to create a new collection.
     * Validates ownership of the target group before creation.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    @PostMapping
    private ResponseEntity<CollectionResponse> handleCreate(@RequestBody CreateCollectionRequest request, Authentication authentication) {
        logger.info("Collection create requested: groupId={}", request.groupId());
        String userId = requireUserId(authentication);

        try {
            assertOwnerViaGroup(request.groupId(), userId);
            var requestedCollection = new EuroCoinCollection(request.name(), request.coins(), request.groupId());
            this.collectionStorageService.save(requestedCollection);
            logger.info("Collection created: groupId={}", request.groupId());
            return ResponseEntity.status(HttpStatus.CREATED).body(CollectionResponse.fromDomain(requestedCollection));
        } catch (EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent resource not found", e);
        }
    }

    /**
     * Handles PATCH requests to update an existing collection.
     * Validates ownership of both the source and target groups if the collection is moved.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    @PatchMapping("/{collectionId}")
    private ResponseEntity<CollectionResponse> handleUpdate(
        @PathVariable String collectionId,
        @RequestBody CreateCollectionRequest request,
        Authentication authentication
    ) {
        logger.info("Collection update requested: id={}", collectionId);
        String userId = requireUserId(authentication);

        try {
            var collectionToUpdate = this.collectionStorageService.getById(collectionId);
            assertOwnerViaGroup(collectionToUpdate.getGroupId(), userId);

            if(!collectionToUpdate.getGroupId().equals(request.groupId())) {
                assertOwnerViaGroup(request.groupId(), userId);
            }

            collectionToUpdate.setName(request.name());
            collectionToUpdate.setGroupId(request.groupId());
            this.collectionStorageService.updateMetadata(collectionToUpdate);
            logger.info("Collection updated: id={}", collectionId);
            return ResponseEntity.ok(CollectionResponse.fromDomain(collectionToUpdate));

        } catch (EuroCoinCollectionSaveException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent Resource not found", e);
        }
    }

    /**
     * Handles DELETE requests to remove a collection.
     * Validates ownership of the collection through the group hierarchy.
     *
     * @param exchange the HTTP exchange containing request and response information
     * @throws IOException if an I/O error occurs during request handling
     */
    @DeleteMapping("/{collectionId}")
    private ResponseEntity<Void> handleDelete(@PathVariable String collectionId, Authentication authentication) {
        logger.info("Collection delete requested: id={}", collectionId);
        String userId = requireUserId(authentication);

        try {
            var collectionToDelete = this.collectionStorageService.getById(collectionId);
            assertOwnerViaGroup(collectionToDelete.getGroupId(), userId);

            this.collectionStorageService.delete(collectionId);
            logger.info("Collection deleted: id={}", collectionId);
            return ResponseEntity.noContent().build();
        } catch (EuroCoinCollectionNotFoundException | EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionDeleteException | EuroCoinCollectionGetByIdException | EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionCoinsLoadException e) {
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Checks whether the authenticated user owns the given group.
     *
     * @param groupId the group to verify
     * @param userId the authenticated user
     */
    private void assertOwnerViaGroup(String groupId, String userId) {
        var group = this.groupStorageService.getById(groupId);

        if (!group.getOwnerId().equals(userId)) {
            logger.warn("Collection access denied: groupId={}, userId={}", groupId, userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

        /**
     * Extracts the user id from the Spring Security authentication.
     */
    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Collection request failed: authentication missing");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getPrincipal().toString();
    }
}
