import { writable } from 'svelte/store';
import type { User } from '$lib/types/user';
import { authService } from '$lib/services/auth.service';
import { isAxiosError } from 'axios';
import { loadGroups, setGroups } from './group.store';


export const currentUser = writable<User | null>(null);
export const isAuthenticated = writable<boolean>(false);
export const authError = writable<string | null>(null);

/**
 * Authenticates user and loads their groups
 * @param username - Username to login with
 */
export async function login(username: string) {
    authError.set(null);
    try {
        await authService.login({username});
        currentUser.set({username});
        isAuthenticated.set(true);
        loadGroups();
    } catch (error) {
        isAuthenticated.set(false);
        currentUser.set(null);
        if (isAxiosError(error)) {
            authError.set(error.response?.data?.message || "Login failed");
        } else {
            authError.set("Unknown error");
        }
    }
}

/**
 * Registers new user account
 * @param username - Username to register
 */
export async function register(username: string) {
    authError.set(null);
    try {
        await authService.register({username});
        currentUser.set({username});
        isAuthenticated.set(true);
    } catch (error) {
        isAuthenticated.set(false);
        currentUser.set(null);
        if (isAxiosError(error)) {
            authError.set(error.response?.data?.message || "Register failed");
        } else {
            authError.set("Unknown error");
        }
    }
}

/**
 * Logs out user and clears session data
 */
export async function logout() {
    authError.set(null);
    try {
        await authService.logout();
        setGroups([]);
    } catch (error) {
        if (isAxiosError(error)) {
            authError.set(error.response?.data?.message || "Logout fehlgeschlagen");
        } else {
            authError.set("Unknown error");
        }
    } finally {
        isAuthenticated.set(false);
        currentUser.set(null);
    }
}