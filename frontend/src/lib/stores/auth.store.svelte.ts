import { writable } from 'svelte/store';
import type { User } from '$lib/types/user';
import { authService } from '$lib/services/auth.service';
import { isAxiosError } from 'axios';
import { loadGroups, setGroups } from './group.store.svelte';

export const authState = $state<{
    currentUser: User | null
    isAuthenticated: boolean
    authError: String | null
    successMessage: String | null
}>({
    currentUser: null,
    isAuthenticated: false,
    authError: null,
    successMessage: null,
});

/**
 * Authenticates user and loads their groups
 * @param username - Username to login with
 */
export async function login(username: string, password: string) {
    authState.authError = null;
    try {
        await authService.login({ username, password });
        authState.currentUser = {username};
        authState.isAuthenticated = true;
        loadGroups();
    } catch (error) {
        authState.isAuthenticated = false;
        authState.currentUser = null;
        if (isAxiosError(error)) {
            authState.authError = error.response?.data?.message || "Login failed";
        } else {
            authState.authError = "Unknown error";
        }
    }
}

/**
 * Registers new user account
 * @param username - Username to register
 */
export async function register(username: string, password: string) {
    authState.authError = null;
    try {
        await authService.register({ username, password });
        authState.currentUser = {username};
        authState.isAuthenticated = true;
    } catch (error) {
        authState.isAuthenticated = false;
        authState.currentUser = null;
        if (isAxiosError(error)) {
            authState.authError = error.response?.data?.message || "Register failed";
        } else {
            authState.authError = "Unknown error";
        }
    }
}

/**
 * Logs out user and clears session data
 */
export async function logout() {
    authState.authError = null;
    try {
        await authService.logout();
        setGroups([]);
    } catch (error) {
        if (isAxiosError(error)) {
            authState.authError = error.response?.data?.message || "Logout fehlgeschlagen";
        } else {
            authState.authError = "Unknown error";
        }
    } finally {
        authState.isAuthenticated = false;
        authState.currentUser = null;
    }
}

export async function setupPassword(username: string, newPassword: string): Promise<boolean> {
    authState.authError = null
    authState.successMessage = null
    try {
        await authService.setupPassword({username, newPassword});
        authState.successMessage = 'Passwort erfolgreich eingerichtet.';
        return true;
    } catch (error) {
        if (isAxiosError(error)) {
            authState.authError = error.response?.data?.message || "Password setup failed";
        } else {
            authState.authError = "Unknown error";
        }
        return false;
    }
}
