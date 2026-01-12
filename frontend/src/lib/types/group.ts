import type { Collection } from "./collection"

/** A group containing multiple collections */
export interface Group{
    id: string
    name: string
    collections: Collection[]
}