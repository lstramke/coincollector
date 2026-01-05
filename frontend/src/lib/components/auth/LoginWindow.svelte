<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';
    import { authError, isAuthenticated, login } from '$lib/stores/auth.store';


    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
    ];

    async function submit(items: AuthField[]) {
        const username = items.find( f => f.id === "username")?.value ?? "";
        if(!username.trim()) {
            authError.set("Username must be not empty");
            return;
        }
        await login(username);     
    }

</script>

<AuthForm
    title="Anmelden"
    description="Melden Sie sich mit Ihrem Konto an"
    submitLabel="Anmelden"
    {fields}
    onSubmit={submit}
/>

{#if $authError}
    <div class="error">{$authError}</div>
{/if}