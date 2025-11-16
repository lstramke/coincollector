import type { Coin } from "./coin"

export interface Collection{
    id: string;
    name: string;
    coins: Coin[]
}