import { writable } from 'svelte/store';
import type { User } from '$lib/types/user';
import { authService } from '$lib/services/auth.service';
import { isAxiosError } from 'axios';


export const currentUser = writable<User | null>(null);
export const isAuthenticated = writable<boolean>(false);
export const authError = writable<string | null>(null);

export async function login(username: string) {
    authError.set(null);
    try {
        await authService.login({username});
        currentUser.set({username});
        isAuthenticated.set(true);
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

export async function logout() {
    authError.set(null);
    try {
        await authService.logout();
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