import { writable } from "svelte/store";
import type { Group } from '$lib/types/group'
import type { Collection } from "$lib/types/collection";
import type { Coin } from "$lib/types/coin";

export const groups = writable<Group[]>();
export const collectionMap = writable<Record<string, Collection>>({});
export const coinMap = writable<Record<string, Coin>>({});

export function setGroups(data: Group[]) {
    groups.set(data);

    const collections: Record<string, Collection> = {};
    const coins: Record<string, Coin> = {};

    for (const group of data) {
        for (const collection of group.collections) {
            collections[collection.id] = collection;
            for (const coin of collection.coins) {
                coins[coin.id] = coin;
            }
        }
    }
    collectionMap.set(collections);
    coinMap.set(coins);
}

setGroups([{
        id: 'g-de',
        name: 'Deutsche Euromünzen',
        collections: [
            {
                id: 'c-de-2015',
                name: 'Deutschland 2015',
                coins: [
                    { id: 'coin-de-2015-2e', value: 2, country: 'Deutschland', year: 2015, mintCity: 'München', description: 'Gedenkmünze Deutsche Einheit' },
                    { id: 'coin-de-2015-1e', value: 1, country: 'Deutschland', year: 2015, mintCity: 'Berlin', description: 'Bundesadler' }
                ]
            },
            {
                id: 'c-de-2020',
                name: 'Deutschland 2020',
                coins: [
                    { id: 'coin-de-2020-50c', value: 0.5, country: 'Deutschland', year: 2020, mintCity: null, description: 'Brandenburger Tor' }
                ]
            }
        ]
    },
    {
        id: 'g-fr',
        name: 'Französische Euromünzen',
        collections: [
            {
                id: 'c-fr-2020',
                name: 'Frankreich 2020',
                coins: [
                    { id: 'coin-fr-2020-2e', value: 2, country: 'Frankreich', year: 2020, mintCity: 'Paris', description: 'Marianne' }
                ]
            }
        ]
    },
    {
        id: 'g-sp',
        name: 'Spanische Euromünzen',
        collections: [
            
        ]
    }
])