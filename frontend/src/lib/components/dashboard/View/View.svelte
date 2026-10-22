<script lang="ts">
    import { selection } from "$lib/stores/selection.store.svelte";
    import GroupView from "./GroupView.svelte";
    import CollectionView from "./CollectionView.svelte";
</script>

<div class="flex h-full w-full flex-col bg-[var(--bg-main)] p-10 max-h-screen overflow-y-auto">
    {#if selection.current?.type === 'collection'}
        <CollectionView />
    {:else if selection.current?.type === 'group'}
        <GroupView />
    {:else}
        <div class="flex flex-1 items-center justify-center">
            <p class="text-lg text-[var(--text-secondary)]">Bitte eine Gruppe oder Collection auswählen.</p>
        </div>
    {/if}
</div>