<script lang="ts">
    import type { Group } from "$lib/types/group";
    import { selection, handleSelectCollection, handleGroupSelect } from "$lib/stores/selection.store";

    import SidebarHeader from "./SidebarHeader.svelte";
    import GroupDropdown from "./GroupDropdown.svelte";
    import SidebarFooter from "./SidebarFooter.svelte";

    let { groups } = $props<{ groups: Group[] }>();

    const collectionCount = $derived(
        groups.reduce((sum: any, group: { collections: string | any[]; }) => sum + group.collections.length, 0)
    );
    const groupCount = $derived(groups.length);

    const selectedId = $derived(
        $selection?.type === "collection" ? $selection.id : undefined
    );

</script>

<aside class="flex h-screen flex-col bg-[var(--bg-sidebar)] text-[var(--text-white)]">
    <SidebarHeader />
    <nav class="flex-1 space-y-3 overflow-y-auto px-4 py-6">
        {#each groups as group (group.id)}
            <GroupDropdown
                {group}
                {selectedId}
                onCollectionSelect={handleSelectCollection}
                onGroupSelect={() => handleGroupSelect(group.id)}
            />
        {/each}
    </nav>
    <SidebarFooter
        {collectionCount}
        {groupCount}
    />
</aside>
