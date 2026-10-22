import type { Collection } from "$lib/types/collection";
import { collectionService } from "$lib/services/collection.service";
import { loadGroup } from "./group.store.svelte";

export const collectionState = $state<{
    collections: Collection[];
    collectionMap: Record<string, Collection>;
}>({
    collections: [],
    collectionMap: {}
});

/**
 * Sets collections and synchronizes the collection map
 */
export function setCollections(collections: Collection[]) {
    collectionState.collections = collections;
    collectionState.collectionMap = Object.fromEntries(
        collections.map(collection => [collection.id, collection])
    );
}

/**
 * Fetches a collection by ID and caches it
 * @param id - Collection ID
 * @returns Collection or undefined if not found
 */
export async function getCollection(id: string): Promise<Collection | undefined> {
    try {
        const col = await collectionService.getCollection(id);
        const current = collectionState.collections;
        const exists = current.some(c => c.id === col.id);

        if (exists) {
            setCollections(
                current.map(c => c.id === col.id ? col : c)
            );
        } else {
            setCollections([...current, col]);
        }

        if (col.groupId) {
            await loadGroup(col.groupId);
        }

        return col;
    } catch {
        return undefined;
    }
}

/**
 * Creates a new collection
 * @returns true if successful, false otherwise
 */
export async function createCollection(data: Parameters<typeof collectionService.createCollection>[0]): Promise<boolean> {
    try {
        const col = await collectionService.createCollection(data);

        setCollections([...collectionState.collections, col]);

        if (col.groupId) {
            await loadGroup(col.groupId);
        }

        return true;
    } catch {
        return false;
    }
}

/**
 * Updates an existing collection
 * @param id - Collection ID
 * @returns true if successful, false otherwise
 */
export async function updateCollection(
    id: string,
    data: Parameters<typeof collectionService.updateCollection>[1]
): Promise<boolean> {
    try {
        const prev = collectionState.collections.find(c => c.id === id);
        const updated = await collectionService.updateCollection(id, data);

        const arr = collectionState.collections.map(c =>
            c.id === id ? { ...c, ...updated } : c
        );

        setCollections(arr);

        const oldGroupId = prev?.groupId;
        const newGroupId = updated.groupId;

        const groupIdsToUpdate =
            oldGroupId && oldGroupId !== newGroupId
                ? [oldGroupId, newGroupId]
                : [newGroupId];

        for (const groupId of groupIdsToUpdate) {
            if (groupId) {
                await loadGroup(groupId);
            }
        }

        return true;
    } catch {
        return false;
    }
}

/**
 * Deletes a collection by ID
 * @param id - Collection ID
 * @returns true if successful, false otherwise
 */
export async function deleteCollection(id: string): Promise<boolean> {
    try {
        const prev = collectionState.collections.find(c => c.id === id);

        await collectionService.deleteCollection(id);

        setCollections(
            collectionState.collections.filter(c => c.id !== id)
        );

        if (prev?.groupId) {
            await loadGroup(prev.groupId);
        }

        return true;
    } catch {
        return false;
    }
}
