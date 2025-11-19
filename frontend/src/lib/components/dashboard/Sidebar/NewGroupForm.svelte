<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import type { DialogField } from "$lib/types/dialogField";

    let dialog: Dialog;

    let fields: DialogField[] = [
        {
            id: "group",
            label: "Gruppenname*",
            value: "",
            type: "text",
            required: true,
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
    title="Neue Gruppe erstellen"
    description="Erstellen Sie eine neue Gruppe für Ihre Collections."
    {fields}
    buttonText="Gruppe erstellen"
    {onSubmit}
/>
