<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import { groups } from "$lib/stores/group.store";
    import type { DialogField } from "$lib/types/dialogField";

    let dialog: Dialog;

    const groupOptions = Object.fromEntries($groups.map(g => [g.name, g.id]));
    const groupNames = Object.keys(groupOptions)
    
    let fields: DialogField[] = [
        {
            id: "group",
            label: "Gruppe*",
            value: "",
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
            placeholder: "Land eingeben",
            options: Object.entries($coinCountryMap).map(([name, code]) => name)
        },
        {
            id: "year",
            label: "Jahr*",
            value: "",
            type: "number",
            required: true,
            placeholder: "Jahr eingeben"
        }
    ];

    function onSubmit(fields: DialogField[]){
       for (let index = 0; index < fields.length; index++) {
            console.log(fields[index].value);
       }
    }

    export function show() {
        dialog.show();
    }

    export function close() {
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
/>
