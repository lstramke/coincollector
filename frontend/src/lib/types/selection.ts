/** Represents the currently selected group or collection */
export type Selection = { type: 'group'; id: string} | { type: 'collection'; id: string} | undefined;