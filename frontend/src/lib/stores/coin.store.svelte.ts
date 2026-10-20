import type { Coin } from "$lib/types/coin";
import { coinService } from "$lib/services/coin.service";
import { getCollection } from "./collection.store.svelte";

export const coinsState = $state<{
    coins: Coin[];
    coinMap: Record<string, Coin>;
}>({
    coins: [],
    coinMap: {}
});

/**
 * Sets coins and synchronizes the coin map
 */
export function setCoins(coins: Coin[]) {
    coinsState.coins = coins;
    coinsState.coinMap = Object.fromEntries(
        coins.map(coin => [coin.id, coin])
    );
}

/**
 * Fetches a coin by ID and caches it
 * @param id - Coin ID
 * @returns Coin or undefined if not found
 */
export async function getCoin(id: string): Promise<Coin | undefined> {
    const cachedCoin = coinsState.coinMap[id];

    if (cachedCoin) {
        return cachedCoin;
    }

    try {
        const coin = await coinService.getCoin(id);
        const current = coinsState.coins;
        const exists = current.some(c => c.id === coin.id);

        if (!exists) {
            setCoins([...current, coin]);
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

        setCoins([...coinsState.coins, coin]);

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
export async function updateCoin(
    id: string,
    data: Parameters<typeof coinService.updateCoin>[1]
): Promise<boolean> {
    try {
        const prev = coinsState.coins.find(c => c.id === id);
        const updated = await coinService.updateCoin(id, data);

        const arr = coinsState.coins.map(c =>
            c.id === id ? { ...c, ...updated } : c
        );

        setCoins(arr);

        const oldCollectionId = prev?.collectionId;
        const newCollectionId = updated.collectionId;

        const collectionIdsToUpdate =
            oldCollectionId && oldCollectionId !== newCollectionId
                ? [oldCollectionId, newCollectionId]
                : [newCollectionId];

        for (const collectionId of collectionIdsToUpdate) {
            if (collectionId) {
                await getCollection(collectionId);
            }
        }

        return true;
    } catch {
        return false;
    }
}

/**
 * Deletes a coin by ID
 * @param id - Coin ID
 * @returns true if successful, false otherwise
 */
export async function deleteCoin(id: string): Promise<boolean> {
    try {
        const prev = coinsState.coins.find(c => c.id === id);

        await coinService.deleteCoin(id);

        setCoins(
            coinsState.coins.filter(c => c.id !== id)
        );

        if (prev?.collectionId) {
            await getCollection(prev.collectionId);
        }

        return true;
    } catch {
        return false;
    }
}
