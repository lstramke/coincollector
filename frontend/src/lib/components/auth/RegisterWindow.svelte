<script lang="ts">
    import AuthForm from './AuthForm.svelte';
    import type { AuthField } from '$lib/types/authField';

    let fields: AuthField[] = [
        { id: 'username', label: 'Benutzername', value: '', required: true },
    ];

    function submit(items: AuthField[]) {
        // TODO: Registration-Handler
        console.log('login', items);
    }
</script>

<AuthForm
    title="Registrieren"
    description="Erstellen Sie ein Konto"
    submitLabel="Registrieren"
    {fields}
    onSubmit={submit}
/>