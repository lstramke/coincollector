<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';
    import { authState, login } from '$lib/stores/auth.store.svelte';


    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
        { id: 'password', label: 'Passwort', type: 'password', value: '', required: true, addVisibilityToggle: true },
    ];

    async function onSubmit(items: AuthField[]) {
        const username = items.find( f => f.id === "username")?.value ?? "";
        const password = items.find( f => f.id === "password")?.value ?? "";
        if(!username.trim()) {
            authState.authError = "Username must be not empty";
            return;
        }
        if(password.length < 8) {
            authState.authError = "Password must be 8 characters minimum";
            return;
        }
        await login(username, password);
    }

</script>

<AuthForm
    title="Anmelden"
    description="Melden Sie sich mit Ihrem Konto an"
    submitLabel="Anmelden"
    {fields}
    {onSubmit}
/>

{#if authState.authError}
    <div class="flex justify-center mt-4">
        <div class="px-4 py-2 rounded border"
            style="background-color: var(--bg-table); border-color: var(--color-destructive); color: var(--color-destructive);">
            {authState.authError}
        </div>
    </div>
{/if}