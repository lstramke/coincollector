import { writable } from "svelte/store";

export const mintCityMap = writable<Record<string, string>>({
    "Berlin": "A",
    "München": "D",
    "Stuttgart": "F",
    "Karlsruhe": "G",
    "Hamburg": "J"
});