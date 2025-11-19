import { writable } from "svelte/store";

import type { Selection } from "$lib/types/selection"

export const selection = writable<Selection>(undefined)

export function handleSelectCollection(collectionId: string) {
    selection.set({ type: "collection", id: collectionId });
}

export function handleGroupSelect(groupId: string) {
    selection.set({ type: "group", id: groupId });
}