import { writable } from "svelte/store";

export const coinValuesMap = writable<Record<string, number>>({
    "2 Euro": 200,
    "1 Euro": 100,
    "50 Cent": 50,
    "20 Cent": 20,
    "10 Cent": 10,
    "5 Cent": 5,
    "2 Cent": 2,
    "1 Cent": 1
});