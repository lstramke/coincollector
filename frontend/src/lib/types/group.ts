import type { Collection } from "./collection"

export interface Group{
    id: string
    name: string
    collections: Collection[]
}