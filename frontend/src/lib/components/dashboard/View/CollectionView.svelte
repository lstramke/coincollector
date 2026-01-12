<script lang="ts">
    import { selection } from "$lib/stores/selection.store";
    import { collectionMap } from "$lib/stores/collection.store";
    import type { Collection } from "$lib/types/collection";
    import NewCoinForm from "./NewCoinForm.svelte";
    import EditCoinForm from "./EditCoinForm.svelte";
    import { mintCityMap } from "$lib/stores/mintCity.store";
    import { deleteCoin } from "$lib/stores/coin.store";
    import { coinCountryCodeMap } from "$lib/stores/coinCountry.store";

    let collection = $derived((): Collection | undefined => {
        return $selection?.type === "collection"
            ? $collectionMap[$selection.id]
            : undefined;
    });

    function formatValue(value: number) {
        if (value >= 100) return `${value / 100} Euro`;
        return `${value} Cent`;
    }

    let newCoinDialog: NewCoinForm;
    let editCoinDialog: EditCoinForm;
</script>

{#if collection()}
    {@const col = collection()}

    <div class="mb-6 flex items-start justify-between">
        <div>
            <h1 class="text-2xl font-semibold text-[var(--text-primary)]">{col?.name}</h1>
            <div class="mt-1 text-sm text-[var(--color-secondary)]">
                {col?.coins.length} Münze{col?.coins.length === 1 ? '' : 'n'}
            </div>
        </div>

        <div>
            <button
                type="button"
                class="flex items-center gap-2 rounded-md bg-[var(--color-primary)] px-4 py-2 text-sm font-medium text-[var(--text-white)] shadow-sm hover:bg-[var(--color-primary)]/90"
                onclick={() => newCoinDialog.show()}
            >
                <span class="text-lg leading-none mr-2">+</span>
                Münze hinzufügen
            </button>
        </div>
    </div>

    <div class="rounded-md bg-[var(--bg-table)] p-0 overflow-hidden">
        <table class="w-full table-fixed text-sm border-collapse">
            <thead class="bg-[var(--bg-table-header)] text-sm font-semibold text-[var(--text-primary)]">
                <tr class="h-12">
                    <th class="px-4 text-left w-[110px]">Wert</th>
                    <th class="px-4 text-left">Prägeland</th>
                    <th class="px-4 text-left w-[96px]">Prägejahr</th>
                    <th class="px-4 text-left w-[140px]">Prägestadt</th>
                    <th class="px-4 text-left">Beschreibung</th>
                    <th class="px-4 pr-6 text-right">Aktionen</th>
                </tr>
            </thead>

            <tbody>
                {#each col?.coins as coin (coin.id)}
                    <tr class="bg-[var(--bg-white)] border-b border-[var(--border-table)]">
                        <td class="px-4 py-3 align-middle">
                            <span class="inline-block rounded-full border border-[var(--border-table)] bg-[var(--bg-main)] px-3 py-1 text-xs font-medium text-[var(--text-primary)]">
                                {formatValue(coin.value)}
                            </span>
                        </td>

                        <td class="px-4 py-3 align-middle text-[var(--text-primary)]">
                            {$coinCountryCodeMap[coin.country]}
                        </td>

                        <td class="px-4 py-3 align-middle text-[var(--text-primary)]">
                            {coin.year}
                        </td>

                        <td class="px-4 py-3 align-middle text-[var(--text-secondary)]">
                            {coin.mint ? $mintCityMap[coin.mint] : '-'}
                        </td>

                        <td class="px-4 py-3 align-middle text-[var(--text-primary)]">
                            {coin.description}
                        </td>

                        <td class="px-4 py-3 align-middle">
                            <div class="flex justify-end gap-2">
                                 <button
                                     class="p-1 rounded text-[var(--color-primary)] hover:bg-[var(--hover-state)]/10 hover:scale-130"
                                     title="Bearbeiten"
                                     aria-label="Bearbeiten"
                                     onclick={() => editCoinDialog.show(coin)}
                                >
                                    <svg class="w-5.5 h-5.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L6.832 19.82a4.5 4.5 0 0 1-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 0 1 1.13-1.897L16.863 4.487Zm0 0L19.5 7.125"/>
                                    </svg>
                                </button>
                                <button
                                    class="p-1 rounded text-[var(--color-destructive)] hover:bg-[var(--hover-state)]/10 hover:scale-130"
                                    title="Löschen"
                                    aria-label="Löschen"
                                    onclick={() => deleteCoin(coin.id)}
                                >
                                    <svg class="w-5.5 h-5.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
                                       <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0"/>
                                    </svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                {/each}
            </tbody>
        </table>
    </div>
{:else}
    <div class="flex flex-1 items-center justify-center">
        <p class="text-lg text-[var(--color-secondary)]">Keine Collection ausgewählt.</p>
    </div>
{/if}

<NewCoinForm bind:this={newCoinDialog}/>
<EditCoinForm bind:this={editCoinDialog}/>