<script lang="ts">
    import NewCollectionForm from "./NewCollectionForm.svelte";
    import NewGroupForm from "./NewGroupForm.svelte";

    const {
        collectionCount,
        groupCount,
    } = $props<{
        collectionCount: number;
        groupCount: number;
    }>();

    let newCollectionDialog: NewCollectionForm;
    let newGroupDialog: NewGroupForm;
</script>

<footer class="border-t border-[var(--border-transparent)] px-4 py-5 text-[var(--text-white)]">
    <div class="space-y-2">
        <button
            class="flex w-full items-center gap-3 rounded-lg border border-[var(--border-transparent)] bg-[var(--btn-background)] px-3 py-2 text-sm font-medium transition hover:bg-[var(--btn-hover)]"
            type="button"
            onclick={() => newGroupDialog.show()}
        >
            <span class="flex h-6 w-6 items-center justify-center rounded-md text-base leading-none text-[var(--text-white)]">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 10.5V16.5M15 13.5H9M13.0607 6.31066L10.9393 4.18934C10.658 3.90804 10.2765 3.75 9.87868 3.75H4.5C3.25736 3.75 2.25 4.75736 2.25 6V18C2.25 19.2426 3.25736 20.25 4.5 20.25H19.5C20.7426 20.25 21.75 19.2426 21.75 18V9C21.75 7.75736 20.7426 6.75 19.5 6.75H14.1213C13.7235 6.75 13.342 6.59197 13.0607 6.31066Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </span>
            Neue Gruppe
        </button>
        <button
            class="flex w-full items-center gap-3 rounded-lg border border-[var(--border-transparent)] bg-[var(--btn-background)] px-3 py-2 text-sm font-medium transition hover:bg-[var(--btn-hover)]"
            type="button"
            onclick={() => newCollectionDialog.show()}
        >
            <span class="flex h-6 w-6 items-center justify-center rounded-md text-base leading-none text-[var(--text-white)]">+</span>
            Neue Collection
        </button>

    </div>

    <div class="mt-4 border-t border-[var(--border-transparent)] pt-3 text-xs leading-5 text-[var(--text-white)]/70">
        {collectionCount} Collections<br />
        {groupCount} Gruppen
    </div>
</footer>

<NewCollectionForm bind:this={newCollectionDialog} />
<NewGroupForm bind:this={newGroupDialog}/>
