<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { coinCountryMap } from "$lib/stores/coinCountryStore";
    import { groups, groupMap } from "$lib/stores/groupStore";
    import type { Collection } from "$lib/types/collection";
    import type { DialogField } from "$lib/types/dialogField";

    let dialog: Dialog;

    let collection = $state<Collection | undefined>(undefined);

    const groupOptions = Object.fromEntries($groups.map(g => [g.name, g.id]));
    const groupNames = Object.keys(groupOptions)
    
    let fields: DialogField[] = $derived.by(() => {
            if (!collection) return [];
            return [
            {
                id: "group",
                label: "Gruppe*",
                value: collection ? $groupMap[collection.groupId].name : "",
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

    function onSubmit(fields: DialogField[]){
       for (let index = 0; index < fields.length; index++) {
            console.log(fields[index].value);
       }
    }

    export function show(collectionToEdit: Collection) {
        collection = collectionToEdit
        dialog.show();
    }

    export function close() {
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
/>
