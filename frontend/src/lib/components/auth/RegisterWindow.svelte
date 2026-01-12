<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';
    import { authError, register } from '$lib/stores/auth.store';

    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
    ];

    async function onSubmit(items: AuthField[]) {
        const username = items.find( f => f.id === "username")?.value ?? "";
        if(!username.trim()) {
            authError.set("Username must be not empty");
            return;
        }
        await register(username);     
    }

</script>

<AuthForm
    title="Registrieren"
    description="Erstellen Sie ein Konto"
    submitLabel="Registrieren"
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
{/if}