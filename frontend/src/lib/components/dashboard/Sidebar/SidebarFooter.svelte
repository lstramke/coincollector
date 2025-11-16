<script lang="ts">
    const {
        collectionCount,
        groupCount,
    } = $props<{
        collectionCount: number;
        groupCount: number;
    }>();
</script>

<footer class="border-t border-[var(--border-transparent)] px-4 py-5 text-[var(--text-white)]">
    <div class="space-y-2">
        <button
            class="flex w-full items-center gap-3 rounded-lg border border-[var(--border-transparent)] bg-[var(--btn-background)] px-3 py-2 text-sm font-medium transition hover:bg-[var(--btn-hover)]"
            type="button"
        >
            <span class="flex h-6 w-6 items-center justify-center rounded-md text-base leading-none text-[var(--text-white)]">+</span>
            Neue Collection
        </button>
        <button
            class="flex w-full items-center gap-3 rounded-lg border border-[var(--border-transparent)] bg-[var(--btn-background)] px-3 py-2 text-sm font-medium transition hover:bg-[var(--btn-hover)]"
            type="button"
        >
            <span class="flex h-6 w-6 items-center justify-center rounded-md text-base leading-none text-[var(--text-white)]">+</span>
            Neue Gruppe
        </button>
    </div>

    <div class="mt-4 border-t border-[var(--border-transparent)] pt-3 text-xs leading-5 text-[var(--text-white)]/70">
        {collectionCount} Collections<br />
        {groupCount} Gruppen
    </div>
</footer>