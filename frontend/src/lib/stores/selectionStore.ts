import { writable } from "svelte/store";

import type { Selection } from "$lib/types/selection"

export const selection = writable<Selection>(undefined)