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
    const countryNames = $derived(Object.keys($coinCountryMap || {}));

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
            id: "country",
            label: "Land*",
            value: "",
            type: "select" as const,
            required: true,
            placeholder: "Land eingeben",
            options: countryNames.sort()
        },
        {
            id: "year",
            label: "Jahr*",
            value: "",
            type: "number" as const,
            required: true,
            placeholder: "Jahr eingeben"
        }
    ]);

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]) {
        errorText = null;

        const groupName = flds.find(f => f.id === 'group')?.value as string;
        const countryName = flds.find(f => f.id === 'country')?.value as string;
        const yearRaw = flds.find(f => f.id === 'year')?.value;

        if (!groupName || !countryName || !yearRaw) {
            errorText = 'Bitte alle Felder ausfüllen.';
            return;
        }

        const groupId = groupOptions[groupName];
        const countryCode = $coinCountryMap[countryName];
        const year = parseInt(String(yearRaw), 10);

        if (!groupId) {
            errorText = 'Ausgewählte Gruppe nicht gefunden.';
            return;
        }
        if (!countryCode) {
            errorText = 'Ausgewähltes Land nicht gefunden.';
            return;
        }
        if (isNaN(year)) {
            errorText = 'Bitte ein gültiges Jahr eingeben.';
            return;
        }

        const name = `${countryName} ${year}`;
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
