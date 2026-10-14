import type { Selection } from "$lib/types/selection"

export const selection = $state({
    current: undefined as Selection
});

export function handleSelectCollection(collectionId: string) {
    selection.current = { type: "collection", id: collectionId };
}

export function handleGroupSelect(groupId: string) {
    selection.current = { type: "group", id: groupId };
}