<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import { groups, groupMap } from "$lib/stores/group.store";
    import { updateCollection } from "$lib/stores/collection.store";
    import type { Collection } from "$lib/types/collection";
    import type { DialogField } from "$lib/types/dialogField";

    let dialog: Dialog;
    let errorText = $state<string | null>(null);

    let collection = $state<Collection | undefined>(undefined);

    const groupOptions = $derived.by(() => Object.fromEntries($groups.map(g => [g.name, g.id])));
    const groupNames = $derived(Object.keys(groupOptions));
    
    let fields: DialogField[] = $derived.by(() => {
            if (!collection) return [];
            return [
            {
                id: "group",
                label: "Gruppe*",
                value: $groupMap[collection.groupId].name,
                type: "select",
                required: true,
                options: groupNames
            },
            {
                id: "country",
                label: "Land*",
                value: "",
                type: "select",
                required: true,
                options: Object.entries($coinCountryMap).map(([name, code]) => name)
            },
            {
                id: "year",
                label: "Jahr*",
                value: "",
                type: "number",
                required: true,
            }
        ];
    });

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]){
        if (!collection) return;
        
        errorText = null;

        const groupName = flds.find(f => f.id === "group")?.value as string;
        const countryName = flds.find(f => f.id === "country")?.value as string;
        const yearRaw = flds.find(f => f.id === "year")?.value;

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
        updateCollection(collection.id, {
            groupId,
            name: name
        }).then(success => {
            if (success) {
                close();
            }
        });
    }

    export function show(collectionToEdit: Collection) {
        collection = collectionToEdit
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
    title="Collection bearbeiten"
    description="Bearbeiten Sie die Daten dieser Collection."
    {fields}
    buttonText="Änderungen speichern"
    {onSubmit}
    {errorText}
/>
