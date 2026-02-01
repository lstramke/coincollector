<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import { groups } from "$lib/stores/group.store";
    import type { DialogField } from "$lib/types/dialogField";
    import { createCollection } from "$lib/stores/collection.store";
    import { get } from "svelte/store";

    let dialog: Dialog;
    let errorText = $state<string | null>(null);

    const groupOptions = $derived.by(() => Object.fromEntries($groups.map(g => [g.name, g.id])));
    const groupNames = $derived($groups.map(g => g.name));

    let fields = $derived.by(() => [
        {
            id: "group",
            label: "Gruppe*",
            value: "",
            type: "select" as const,
            required: true,
            options: groupNames
        },
        {
            id: "collectionName",
            label: "Collection Name*",
            value: "",
            type: "text" as const,
            required: true,
            placeholder: "Name der Collection eingeben"
        },
    ]);

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]) {
        errorText = null;

        const groupName = flds.find(f => f.id === 'group')?.value as string;
        const collectionName = flds.find(f => f.id === 'collectionName')?.value as string;

        if (!groupName || !collectionName) {
            errorText = 'Bitte alle Felder ausfüllen.';
            return;
        }

        const groupId = groupOptions[groupName];
        if (!groupId) {
            errorText = 'Ausgewählte Gruppe nicht gefunden.';
            return;
        }

        const name = collectionName.trim();
        if (!name) {
            errorText = 'Bitte einen gültigen Namen eingeben.';
            return;
        }

        const success = await createCollection({ name, groupId });
        if (success) {
            resetFields();
            close();
        } else {
            errorText = 'Collection konnte nicht erstellt werden.';
        }
    }

    export function show() {
        resetFields();
        dialog.show();
    }

    export function close() {
        resetFields();
        dialog.close();
    }
</script>

<Dialog
    bind:this={dialog}
    title="Neue Collection erstellen"
    description="Erstellen Sie eine neue Collection für Ihre Münzen."
    {fields}
    buttonText="Collection erstellen"
    {onSubmit}
    {errorText}
/>
