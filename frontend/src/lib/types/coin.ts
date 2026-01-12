/** Represents a coin in a collection */
export interface Coin {
    id: string;
    value: number;
    country: string;
    year: number;
    mint?: string | null
    description: string
    collectionId: string
}