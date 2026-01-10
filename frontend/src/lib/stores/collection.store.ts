
import { writable, derived, get } from 'svelte/store';
import type { Collection } from '$lib/types/collection';
import { collectionService } from '$lib/services/collection.service';
import { loadGroup, groups } from './group.store';

export const collections = writable<Collection[]>([]);

export const collectionMap = derived(collections, $collections => {
	const map: Record<string, Collection> = {};
	for (const c of $collections) {
		map[c.id] = c;
	}
	return map;
});

export async function getCollection(id: string): Promise<Collection | undefined> {
	try {
		const col = await collectionService.getCollection(id);
		const current = get(collections);
		collections.set([...current, col]);
		return col;
	} catch {
		return undefined;
	}
}

export async function createCollection(data: Parameters<typeof collectionService.createCollection>[0]): Promise<boolean> {
	try {
		const col = await collectionService.createCollection(data);
		collections.set([...get(collections), col]);
		if (col.groupId) {
			await loadGroup(col.groupId);
		}
		return true;
	} catch {
		return false;
	}
}

export async function updateCollection(id: string, data: Parameters<typeof collectionService.updateCollection>[1]): Promise<boolean> {
	try {
		const prev = get(collections).find(c => c.id === id);
		const updated = await collectionService.updateCollection(id, data);
		const arr = get(collections).map(c => c.id === id ? { ...c, ...updated } : c);
		collections.set(arr);

		const oldGroupId = prev?.groupId;
		const newGroupId = updated.groupId;
		const groupIdsToUpdate = oldGroupId && oldGroupId !== newGroupId ? [oldGroupId, newGroupId] : [newGroupId];
		for (const gid of groupIdsToUpdate) {
			if (gid) {
				await loadGroup(gid);
			}
		}
		return true;
	} catch {
		return false;
	}
}

export async function deleteCollection(id: string): Promise<boolean> {
	try {
		const prev = get(collections).find(c => c.id === id);
		await collectionService.deleteCollection(id);
		collections.set(get(collections).filter(c => c.id !== id));
		if (prev?.groupId) {
			await loadGroup(prev.groupId);
		}
		return true;
	} catch {
		return false;
	}
}
