<script lang="ts">
    import { selection } from "$lib/stores/selection.store";
    import { groups, deleteGroup } from "$lib/stores/group.store";
    import CollectionCard from "./CollectionCard.svelte";
    import EditGroupForm from "./EditGroupForm.svelte";

    let editGroupDialog: EditGroupForm;

    const group = $derived($groups.find(g => g.id === $selection?.id));
</script>

{#if group}
    <div class="flex gap-4 mb-4">
        <div class="flex flex-col">
            <div class="flex items-center gap-2 mb-1">
                <h1 class="text-xl font-semibold">{group.name}</h1>
                <div class="flex items-center gap-1">
                    <button
                        class="p-0.5 rounded text-[var(--color-primary)] hover:bg-[var(--hover-state)]/10 hover:scale-110"
                        title="Bearbeiten"
                        aria-label="Bearbeiten"
                        onclick={() => editGroupDialog.show(group)}
                    >
                        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
                            <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L6.832 19.82a4.5 4.5 0 0 1-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 0 1 1.13-1.897L16.863 4.487Zm0 0L19.5 7.125"/>
                        </svg>
                    </button>
                    <button
                        class="p-0.5 rounded text-[var(--color-destructive)] hover:bg-[var(--hover-state)]/10 hover:scale-110"
                        title="Löschen"
                        aria-label="Löschen"
                        onclick={() => deleteGroup(group.id)}
                    >
                        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
                           <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0"/>
                        </svg>
                    </button>
                </div>
            </div>
            <div class="text-sm text-[var(--color-secondary)]">
                {group.collections.length} Collections ·
                {group.collections.reduce((sum, c) => sum + c.coins.length, 0)} Münzen
            </div>
        </div>
    </div>
    <div class="space-y-4">
        {#each group.collections as collection (collection.id)}
            <CollectionCard {collection} />
        {/each}
    </div>
{:else}
    <p class="text-lg text-[var(--text-secondary)]">Keine Gruppe ausgewählt.</p>
{/if}

<EditGroupForm bind:this={editGroupDialog}/>
