import { collections } from './collection.store';
import { coins } from './coin.store';
import { isAxiosError } from 'axios';
import type { Group } from '$lib/types/group';
import type { Collection } from "$lib/types/collection";
import type { Coin } from "$lib/types/coin";
import { groupService } from '$lib/services/group.service';

export const groupState = $state<{
    error: string | null;
    groups: Group[];
    groupMap: Record<string, Group>;
}>({
    error: null,
    groups: [],
    groupMap: {}
});

/**
 * Sets groups and syncs coins and collections stores
 * @param data - Array of groups with nested collections and coins
 */
export function setGroups(data: Group[]) {
    const sortedGroups = data
        .map(g => ({
            ...g,
            collections: [...g.collections].sort((a: Collection, b: Collection) => a.name.localeCompare(b.name))
        }))
        .sort((a: Group, b: Group) => a.name.localeCompare(b.name));

    groupState.groups = sortedGroups;

    groupState.groupMap = Object.fromEntries(
        sortedGroups.map(group => [group.id, group])
    );

    const allCoins: Coin[] = sortedGroups.flatMap(g => g.collections.flatMap(c => c.coins));
    coins.set(allCoins);

    const allCollections: Collection[] = sortedGroups.flatMap(g => g.collections);
    collections.set(allCollections);
}

/**
 * Loads all groups from server
 */
export async function loadGroups() {
    groupState.error = null;
    try {
        const data = await groupService.getAllGroups();
        setGroups(data);
    } catch (error) {
        if (isAxiosError(error)) {
            groupState.error = error.response?.data?.message || "Failed to load groups";
        } else {
            groupState.error = "Unknown error while loading groups";
        }
    }
}

/**
 * Loads a specific group by ID and updates store
 * @param id - Group ID
 */
export async function loadGroup(id: string): Promise<void> {
    groupState.error = null;
    try {
        const updatedGroup = await groupService.getGroup(id);
        const currentGroups = groupState.groups;
        const exists = currentGroups.some(g => g.id === updatedGroup.id);
        if (exists) {
            setGroups(currentGroups.map(g => g.id === updatedGroup.id ? updatedGroup : g));
        } else {
            setGroups([...currentGroups, updatedGroup]);
        }
    } catch (error) {
        if (isAxiosError(error)) {
            groupState.error = error.response?.data?.message || "Failed to load group";
        } else {
            groupState.error = "Unknown error while loading group";
        }
    }
}

/**
 * Creates a new group
 * @returns true if successful, false otherwise
 */
export async function addGroup(data: Parameters<typeof groupService.createGroup>[0]): Promise<boolean> {
    groupState.error = null;
    try {
        const newGroup = await groupService.createGroup(data);
        const current = groupState.groups;
        setGroups([...current, newGroup]);
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupState.error = error.response?.data?.message || "Failed to create group";
        } else {
            groupState.error = "Unknown error while creating group";
        }
        return false;
    }
}

/**
 * Updates an existing group
 * @param id - Group ID
 * @returns true if successful, false otherwise
 */
export async function updateGroup(id: string, data: Parameters<typeof groupService.updateGroup>[1]): Promise<boolean> {
    groupState.error = null;
    try {
        const updated = await groupService.updateGroup(id, data);
        const current = groupState.groups;
        setGroups(current.map(g => g.id === id ? { ...g, ...updated } : g));
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupState.error = error.response?.data?.message || "Failed to update group";
        } else {
            groupState.error = "Unknown error while updating group";
        }
        return false;
    }
}

/**
 * Deletes a group
 * @param id - Group ID
 * @returns true if successful, false otherwise
 */
export async function deleteGroup(id: string): Promise<boolean> {
    groupState.error = null;
    try {
        await groupService.deleteGroup(id);
        const current = groupState.groups;
        setGroups(current.filter(g => g.id !== id));
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupState.error = error.response?.data?.message || "Failed to delete group";
        } else {
            groupState.error = "Unknown error while deleting group";
        }
        return false;
    }
}