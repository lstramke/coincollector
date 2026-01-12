<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import type { DialogField } from "$lib/types/dialogField";
    import { addGroup, groupError } from "$lib/stores/group.store";

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

    async function onSubmit(fields: DialogField[]){
        const nameField = fields.find(f => f.id === "group");
        if (!nameField || typeof nameField.value !== "string" || !nameField.value.trim()) {
            groupError.set("Bitte einen gültigen Gruppennamen eingeben.");
            return;
        }
        const success = await addGroup({ name: nameField.value.trim() });
        if (success) {
            close();
        }
    }

    export function show() {
        fields[0].value = "";
        dialog.show();
    }

    export function close() {
        fields[0].value = "";
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
    errorText={$groupError}
/>
