export interface Coin {
    id: string;
    value: number;
    country: string;
    year: number;
    mintCity?: string | null
    description: string
}