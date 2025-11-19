import { writable } from 'svelte/store';
import type { User } from '$lib/types/user';

export const currentUser = writable<User | null>(null);
export const isAuthenticated = writable<boolean>(false);
