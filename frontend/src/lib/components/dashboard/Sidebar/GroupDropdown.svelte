<script lang="ts">
    import type { Group } from '$lib/types/group';

    const { group, selectedId, onCollectionSelect, onGroupSelect } = $props<{
        group: Group;
        selectedId?: string;
        onCollectionSelect: (id: string) => void;
        onGroupSelect: () => void;
    }>();

    let open = $state(false);

    function toggleOpen() {
        open = !open;
    }
</script>

<section class="text-sm text-[var(--text-white)]">
    <div class="flex w-full items-center justify-between rounded-lg px-3 py-2">
        <div class="flex items-center gap-2">
            <button
                type="button"
                class="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-[var(--hover-state)]/40"
                aria-label={open ? "Aufklappen schließen" : "Aufklappen öffnen"}
                aria-expanded={open}
                onpointerup={toggleOpen}
            >
                <svg
                    class="h-3 w-3 transition-transform"
                    class:rotate-90={open}
                    viewBox="0 0 10 10"
                    fill="none"
                >
                    <path d="M2 1.5L7 5 2 8.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
            </button>

            <button
                type="button"
                class="flex-1 text-left font-semibold text-[var(--text-white)]/90 transition-colors hover:bg-[var(--hover-state)]/40 px-3 py-2 rounded"
                onpointerup={onGroupSelect}
            >
                {group.name}
            </button>
        </div>

        <span class="rounded-full bg-[var(--text-white)]/15 px-2 py-0.5 text-xs font-medium text-[var(--text-white)]/70">
            {group.collections.length}
        </span>
    </div>

    {#if open}
        <ul class="mt-1 space-y-1 pl-9">
            {#each group.collections as collection (collection.id)}
                <li>
                    <button
                        type="button"
                        class="flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm text-[var(--text-white)]/85 transition-colors hover:bg-[var(--hover-state)]/35 data-[active=true]:bg-[var(--active-selection)] data-[active=true]:text-[var(--text-primary)]"
                        data-active={collection.id === selectedId}
                        onpointerup={() => onCollectionSelect(collection.id)}
                    >
                        <span>{collection.name}</span>
                        <span class="text-xs text-[var(--text-white)]/60 data-[active=true]:text-[var(--text-primary)]/70">
                            {collection.coins.length} Münzen
                        </span>
                    </button>
                </li>
            {/each}
        </ul>
    {/if}
</section>

