<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';
    import { isAuthenticated } from '$lib/stores/userStore';

    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
    ];

    function submit(items: AuthField[]) {
        if (items.every(f => f.value.trim() !== '')) {
            isAuthenticated.set(true)
        }
    }
</script>

<AuthForm
    title="Anmelden"
    description="Melden Sie sich mit Ihrem Konto an"
    submitLabel="Anmelden"
    {fields}
    onSubmit={submit}
/>