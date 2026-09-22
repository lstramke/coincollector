<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';
    import { authError, successMessage, setupPassword } from '$lib/stores/auth.store';

    let { onSuccess }: { onSuccess: () => void } = $props();

    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
        { id: 'password', label: 'Passwort', type: 'password', value: '', required: true, addVisibilityToggle: true },
        { id: 'passwordConfirm', label: 'Passwort bestätigen', type: 'password', value: '', required: true, addVisibilityToggle: true },
    ];

    async function onSubmit(items: AuthField[]) {
        const username = items.find( f => f.id === "username")?.value ?? "";
        const password = items.find( f => f.id === "password")?.value ?? "";
        const passwordConfirm = items.find( f => f.id === "passwordConfirm")?.value ?? "";
        if(!username.trim()) {
            authError.set("Username must be not empty");
            return;
        }
        if(password.length < 8) {
            authError.set("Password must be 8 characters minimum");
            return;
        }
        if(password !== passwordConfirm) {
            authError.set("Passwords do not match");
            return;
        }
        const success = await setupPassword(username, password);
        if(success) {
            setTimeout(() => {
                onSuccess();
            }, 2000);
        }
    }

</script>

<AuthForm
    title="Passwort einrichten"
    description="Richten Sie für ihr migriertes Konto ein Passwort ein"
    submitLabel="Password einrichten"
    {fields}
    {onSubmit}
/>

{#if $authError}
    <div class="flex justify-center mt-4">
        <div class="px-4 py-2 rounded border"
            style="background-color: var(--bg-table); border-color: var(--color-destructive); color: var(--color-destructive);">
            {$authError}
        </div>
    </div>
{:else if $successMessage}
    <div class="flex justify-center mt-4">
        <div
            class="px-4 py-2 rounded border"
            style="background-color: var(--bg-table); border-color: var(--color-success); color: var(--color-success);"
        >
            {$successMessage}
        </div>
    </div>
{/if}