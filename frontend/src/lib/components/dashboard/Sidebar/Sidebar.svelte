<script lang="ts">
    import type { Group } from "$lib/types/group";
    import { selection } from "$lib/stores/selectionStore";

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

    function handleSelectCollection(id: string) {
        selection.set({ type: "collection", id });
    }

    function handleGroupToggle(groupId: string) {
        selection.set({ type: "group", id: groupId });
    }
</script>

<aside class="flex h-full min-h-screen flex-col bg-[var(--bg-sidebar)] text-[var(--text-white)]">
    <SidebarHeader />
    <nav class="flex-1 space-y-3 overflow-y-auto px-4 py-6">
        {#each groups as group (group.id)}
            <GroupDropdown
                {group}
                {selectedId}
                onCollectionSelect={handleSelectCollection}
                onGroupSelect={() => handleGroupToggle(group.id)}
            />
        {/each}
    </nav>
    <SidebarFooter
        {collectionCount}
        {groupCount}
    />
</aside>
