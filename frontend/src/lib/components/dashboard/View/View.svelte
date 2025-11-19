<script lang="ts">
    import { selection } from "$lib/stores/selectionStore";
    import GroupView from "./GroupView.svelte";
    import CollectionView from "./CollectionView.svelte";
</script>

<div class="flex h-full w-full flex-col bg-[var(--bg-main)] p-10">
    {#if $selection?.type === 'collection'}
        <CollectionView />
    {:else if $selection?.type === 'group'}
        <GroupView />
    {:else}
        <div class="flex flex-1 items-center justify-center">
            <p class="text-lg text-[var(--text-secondary)]">Bitte eine Gruppe oder Collection auswählen.</p>
        </div>
    {/if}
</div>