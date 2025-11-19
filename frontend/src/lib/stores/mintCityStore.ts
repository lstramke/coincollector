import { writable } from "svelte/store";

export const cityMintMap = writable<Record<string, string>>({
    "Berlin": "A",
    "München": "D",
    "Stuttgart": "F",
    "Karlsruhe": "G",
    "Hamburg": "J"
});

export const mintCityMap = writable<Record<string, string>>({
    "A": "Berlin",
    "D": "München",
    "F": "Stuttgart",
    "G": "Karlsruhe",
    "J": "Hamburg"
});