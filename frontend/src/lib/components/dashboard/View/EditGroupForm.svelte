<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { updateGroup } from "$lib/stores/group.store";
    import type { Group } from "$lib/types/group";
    import type { DialogField } from "$lib/types/dialogField";

    let dialog: Dialog;
    let errorText = $state<string | null>(null);

    let group = $state<Group | undefined>(undefined);

    let fields: DialogField[] = $derived.by(() => {
            if (!group) return [];
            return [
            {
                id: "name",
                label: "Name*",
                value: group?.name || "",
                type: "text",
                required: true,
            }
        ];
    });

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]){
       if (!group) return;
       
       errorText = null;

       const name = flds.find(f => f.id === "name")?.value as string;

       if (!name) {
           errorText = 'Bitte einen Namen eingeben.';
           return;
       }

       const success = await updateGroup(group.id, { name });
       
       if (success) {
           resetFields();
           close();
       } else {
           errorText = 'Gruppe konnte nicht aktualisiert werden.';
       }
    }

    export function show(groupToEdit: Group) {
        group = groupToEdit
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
    title="Gruppe bearbeiten"
    description="Bearbeiten Sie den Namen dieser Gruppe."
    {fields}
    buttonText="Änderungen speichern"
    onSubmit={onSubmit}
    {errorText}
/>
