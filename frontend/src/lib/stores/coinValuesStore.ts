import { writable } from "svelte/store";

export const stringCoinValuesMap = writable<Record<string, number>>({
    "2 Euro": 200,
    "1 Euro": 100,
    "50 Cent": 50,
    "20 Cent": 20,
    "10 Cent": 10,
    "5 Cent": 5,
    "2 Cent": 2,
    "1 Cent": 1
});

export const coinValuesStringMap = writable<Record<number, string>>({
    200 : "2 Euro",
    100 : "1 Euro",
    50 : "50 Cent",
    20 : "20 Cent",
    10 : "10 Cent",
    5 : "5 Cent",
    2 : "2 Cent",
    1 : "1 Cent"
});