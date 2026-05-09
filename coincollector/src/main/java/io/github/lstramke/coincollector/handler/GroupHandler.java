package io.github.lstramke.coincollector.handler;

import java.util.List;

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

/**
 * Handler for collection group-related HTTP requests.
 * Manages CRUD operations for Euro coin collection groups.
 * Validates ownership and authorization for all group operations.
 */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupHandler {

    private final EuroCoinCollectionGroupStorageService groupStorageService;
    private final static Logger logger = LoggerFactory.getLogger(GroupHandler.class);

/**
     * Constructs a new GroupHandler.
     *
     * @param groupStorageService service for group storage
     */
    @Autowired
    public GroupHandler(EuroCoinCollectionGroupStorageService groupStorageService) {
        this.groupStorageService = groupStorageService;
    }

    /**
     * Retrieves all groups for the authenticated user.
     *
     * @param authentication the authenticated user
     * @return list of groups
     * @throws ResponseStatusException if retrieval fails
     */
    @GetMapping
    private ResponseEntity<List<GroupsResponse>> handleGetAll(Authentication authentication) {
        logger.info("Group read requested for user");
        String userId = requireUserId(authentication);

        try {
            var allGroupsForUser = this.groupStorageService.getAllByUser(userId);
            logger.info("Groups read: count={}", allGroupsForUser.size());
            return ResponseEntity.ok(
                allGroupsForUser.stream()
                .map(GroupsResponse::fromDomain)
                .toList()
            );
        } catch (EuroCoinCollectionGroupGetAllException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }    

    /**
     * Creates a new group.
     *
     * @param request the group creation request
     * @param authentication the authenticated user
     * @return the created group response
     * @throws ResponseStatusException if creation fails
     */
    @PostMapping
    private ResponseEntity<GroupsResponse> handleCreate(@RequestBody CreateGroupRequest request, Authentication authentication) {
        logger.info("Group create requested: name={}", request.name());
        String userId = requireUserId(authentication);

        try {
            var requestedGroup = new EuroCoinCollectionGroup(request.name(), userId);
            this.groupStorageService.save(requestedGroup);
            logger.info("Group created: id={}", requestedGroup.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(GroupsResponse.fromDomain(requestedGroup));
        } catch (EuroCoinCollectionGroupSaveException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Retrieves a group by ID.
     *
     * @param groupId the group ID
     * @param authentication the authenticated user
     * @return the group response
     * @throws ResponseStatusException if group not found or unauthorized
     */
    @GetMapping("/{groupId}")
    private ResponseEntity<GroupsResponse> handleGetWithId(@PathVariable String groupId, Authentication authentication) {
        logger.info("Group read requested: id={}", groupId);
        String userId = requireUserId(authentication);

        try {
            var group = this.groupStorageService.getById(groupId);
            assertOwnerViaGroup(group, userId);
            return ResponseEntity.ok(GroupsResponse.fromDomain(group));
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGroupGetByIdException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Updates an existing group.
     *
     * @param groupId the group ID
     * @param request the update request
     * @param authentication the authenticated user
     * @return the updated group response
     * @throws ResponseStatusException if group not found or unauthorized
     */
    @PatchMapping("/{groupId}")
    private ResponseEntity<GroupMetadataResponse> handleUpdate(
        @PathVariable String groupId, 
        @RequestBody UpdateGroupRequest request, 
        Authentication authentication 
    ) {
        logger.info("Group update requested: id={}", groupId);
        String userId = requireUserId(authentication);

        try {
            var groupToUpdate = this.groupStorageService.getById(groupId);
            assertOwnerViaGroup(groupToUpdate, userId);

            groupToUpdate.setName(request.name());
            this.groupStorageService.updateMetadata(groupToUpdate);
            logger.info("Group updated: id={}", groupId);

            var response = new GroupMetadataResponse(groupToUpdate.getName());
            return ResponseEntity.ok(response);
            
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        } catch (EuroCoinCollectionGroupGetByIdException | EuroCoinCollectionGroupUpdateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
    }

    /**
     * Deletes a group by ID.
     *
     * @param groupId the group ID
     * @param authentication the authenticated user
     * @return no content response
     * @throws ResponseStatusException if group not found or unauthorized
     */
    @DeleteMapping("/{groupId}")
    private ResponseEntity<Void> handleDelete(@PathVariable String groupId, Authentication authentication) {
        logger.info("Group delete requested: id={}", groupId);
        String userId = requireUserId(authentication);

        try {
            var groupToDelete = this.groupStorageService.getById(groupId);
            assertOwnerViaGroup(groupToDelete, userId);

            this.groupStorageService.delete(groupId);
            logger.info("Group deleted: id={}", groupId);
            return ResponseEntity.noContent().build();
        } catch (EuroCoinCollectionGroupDeleteException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        } catch (EuroCoinCollectionGroupNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
        }
    }

    /**
     * Asserts that the user owns the group.
     *
     * @param group the group to check
     * @param userId the user ID
     * @throws ResponseStatusException if user does not own the group
     */
    private void assertOwnerViaGroup(EuroCoinCollectionGroup group, String userId) {
        if (!group.getOwnerId().equals(userId)) {
            logger.warn("Group access denied: groupId={}, userId={}", group.getId(), userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    /**
     * Extracts the user ID from authentication.
     *
     * @param authentication the Spring Security authentication
     * @return the user ID
     * @throws ResponseStatusException if authentication is missing
     */
    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.debug("UserUd required but not existent");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getPrincipal().toString();
    }
    
}
