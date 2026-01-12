import type { Coin } from "./coin"

/** A collection of coins grouped together */
export interface Collection{
    id: string;
    name: string;
    coins: Coin[]
    groupId: string
}