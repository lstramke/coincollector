<script lang="ts">
    import { selection } from "$lib/stores/selection.store";
    import { groups } from "$lib/stores/group.store";
    import CollectionCard from "./CollectionCard.svelte";

    $: group = $groups.find(g => g.id === $selection?.id);
</script>

{#if group}
    <h1 class="text-xl font-semibold mb-1">{group.name}</h1>
    <div class="mb-4 text-sm text-[var(--color-secondary)]">
        {group.collections.length} Collections ·
        {group.collections.reduce((sum, c) => sum + c.coins.length, 0)} Münzen
    </div>
    <div class="space-y-4">
        {#each group.collections as collection (collection.id)}
            <CollectionCard {collection} />
        {/each}
    </div>
{:else}
    <p class="text-lg text-[var(--text-secondary)]">Keine Gruppe ausgewählt.</p>
{/if}