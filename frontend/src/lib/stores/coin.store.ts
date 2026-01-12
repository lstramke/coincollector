import { writable, derived, get } from 'svelte/store';
import type { Coin } from '$lib/types/coin';
import { coinService } from '$lib/services/coin.service';
import { getCollection } from './collection.store';

export const coins = writable<Coin[]>([]);

/**
 * Derived store mapping coin IDs to coin objects for quick lookup
 */
export const coinMap = derived(coins, $coins => {
	const map: Record<string, Coin> = {};
	for (const c of $coins) {
		map[c.id] = c;
	}
	return map;
});

/**
 * Fetches a coin by ID and caches it
 * @param id - Coin ID
 * @returns Coin or undefined if not found
 */
export async function getCoin(id: string): Promise<Coin | undefined> {
	const map = get(coinMap);
	if (map[id]) return map[id];
	try {
		const coin = await coinService.getCoin(id);
		const current = get(coins);
		if (!current.find(c => c.id === coin.id)) {
			coins.set([...current, coin]);
		}
		return coin;
	} catch {
		return undefined;
	}
}

/**
 * Creates a new coin
 * @returns true if successful, false otherwise
 */
export async function createCoin(data: Parameters<typeof coinService.createCoin>[0]): Promise<boolean> {
	try {
		const coin = await coinService.createCoin(data);
		coins.set([...get(coins), coin]);
		if (coin.collectionId) {
			await getCollection(coin.collectionId);
		}
		return true;
	} catch {
		return false;
	}
}

/**
 * Updates an existing coin
 * @param id - Coin ID
 * @returns true if successful, false otherwise
 */
export async function updateCoin(id: string, data: Parameters<typeof coinService.updateCoin>[1]): Promise<boolean> {
	try {
		const prev = get(coins).find(c => c.id === id);
		const updated = await coinService.updateCoin(id, data);
		const arr = get(coins).map(c => c.id === id ? { ...c, ...updated } : c);
		coins.set(arr);

		const oldCollectionId = prev?.collectionId;
		const newCollectionId = updated.collectionId;
		const collectionIdsToUpdate = oldCollectionId && oldCollectionId !== newCollectionId ? [oldCollectionId, newCollectionId] : [newCollectionId];
		for (const cid of collectionIdsToUpdate) {
			if (cid) {
				await getCollection(cid);
			}
		}
		return true;
	} catch {
		return false;
	}
}

/**
 * Deletes a coin
 * @param id - Coin ID
 * @returns true if successful, false otherwise
 */
export async function deleteCoin(id: string): Promise<boolean> {
	try {
		const prev = get(coins).find(c => c.id === id);
		await coinService.deleteCoin(id);
		coins.set(get(coins).filter(c => c.id !== id));
		if (prev?.collectionId) {
			await getCollection(prev.collectionId);
		}
		return true;
	} catch {
		return false;
	}
}
