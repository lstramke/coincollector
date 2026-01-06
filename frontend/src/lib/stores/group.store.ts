import { get, writable, derived } from "svelte/store";
import { collections } from './collection.store';
import { coins } from './coin.store';
import { isAxiosError } from 'axios';
import type { Group } from '$lib/types/group';
import type { Collection } from "$lib/types/collection";
import type { Coin } from "$lib/types/coin";
import { groupService } from '$lib/services/group.service';

export const groupError = writable<string | null>(null);
export const groups = writable<Group[]>([]);

export const groupMap = derived(groups, $groups => {
    const map: Record<string, Group> = {};
    for (const g of $groups) {
        map[g.id] = g;
    }
    return map;
});

export function setGroups(data: Group[]) {
    groups.set(data);

    const allCoins: Coin[] = data.flatMap(g => g.collections.flatMap(c => c.coins));
    coins.set(allCoins);

    const allCollections: Collection[] = data.flatMap(g => g.collections);
    collections.set(allCollections);
}

export async function loadGroups() {
    groupError.set(null);
    try {
        const data = await groupService.getAllGroups();
        setGroups(data);
    } catch (error) {
        if (isAxiosError(error)) {
            groupError.set(error.response?.data?.message || "Failed to load groups");
        } else {
            groupError.set("Unknown error while loading groups");
        }
    }
}

export async function loadGroup(id: string): Promise<void> {
    groupError.set(null);
    try {
        const updatedGroup = await groupService.getGroup(id);
        const currentGroups = get(groups);
        const newGroups = currentGroups.map(g => g.id === updatedGroup.id ? updatedGroup : g);
        groups.set(newGroups);
    } catch (error) {
        if (isAxiosError(error)) {
            groupError.set(error.response?.data?.message || "Failed to load group");
        } else {
            groupError.set("Unknown error while loading group");
        }
    }
}

export async function addGroup(data: Parameters<typeof groupService.createGroup>[0]): Promise<boolean> {
    groupError.set(null);
    try {
        const newGroup = await groupService.createGroup(data);
        const current = get(groups);
        setGroups([...current, newGroup]);
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupError.set(error.response?.data?.message || "Failed to create group");
        } else {
            groupError.set("Unknown error while creating group");
        }
        return false;
    }
}

export async function updateGroup(id: string, data: Parameters<typeof groupService.updateGroup>[1]): Promise<boolean> {
    groupError.set(null);
    try {
        const updated = await groupService.updateGroup(id, data);
        const current = get(groups);
        const updatedGroups = current.map(g => g.id === id ? { ...g, ...updated } : g);
        setGroups(updatedGroups);
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupError.set(error.response?.data?.message || "Failed to update group");
        } else {
            groupError.set("Unknown error while updating group");
        }
        return false;
    }
}

export async function deleteGroup(id: string): Promise<boolean> {
    groupError.set(null);
    try {
        await groupService.deleteGroup(id);
        const current = get(groups);
        setGroups(current.filter(g => g.id !== id));
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            groupError.set(error.response?.data?.message || "Failed to delete group");
        } else {
            groupError.set("Unknown error while deleting group");
        }
        return false;
    }
}