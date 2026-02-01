<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { collectionMap } from "$lib/stores/collection.store";
    import { selection } from "$lib/stores/selection.store";
    import { cityMintMap } from "$lib/stores/mintCity.store";
    import type { DialogField } from "$lib/types/dialogField";
    import { stringCoinValuesMap } from "$lib/stores/coinValues.store";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import { createCoin } from "$lib/stores/coin.store";

    let dialog: Dialog;
    let errorText = $state<string | null>(null);

    const currentCollection = $derived($selection?.id ? $collectionMap[$selection.id] : undefined);

    const countryNames = $derived(Object.keys($coinCountryMap || {}));
    const valueNames = $derived(Object.keys($stringCoinValuesMap || {}));
    const mintCityNames = $derived(Object.entries($cityMintMap || {}).map(([name, code]) => `${name} (${code})`));

    let fields = $derived.by(() => [
        {
            id: "collection",
            label: "Collection*",
            value: currentCollection?.name ?? "",
            type: "text" as const,
            required: true,
            disabled: true
        },
        {
            id: "country",
            label: "Prägeland*",
            value: "",
            type: "select" as const,
            required: true,
            options: countryNames.sort()
        },
        {
            id: "value",
            label: "Wert*",
            value: "",
            type: "select" as const,
            required: true,
            options: valueNames
        },
        {
            id: "year",
            label: "Prägejahr*",
            value: "",
            type: "select" as const,
            required: true,
            options: Array.from({ length: new Date().getFullYear() - 1999 + 1 }, (_, i) => (1999 + i).toString())
        },
        {
            id: "mintCity",
            label: "Prägestadt",
            value: "",
            type: "select" as const,
            options: mintCityNames,
            placeholder: "Prägestadt wählen",
            description: "Aktuell nur für Deutschland unterstützt"
        },
        {
            id: "description",
            label: "Beschreibung",
            value: "",
            type: "text" as const,
            placeholder: "",
            description: "Es wird, wenn es keine Eingabe gibt, eine Beschreibung generiert."
        }
    ]);

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]) {
        errorText = null;

        const collectionId = currentCollection?.id;
        const countryName = flds.find(f => f.id === 'country')?.value as string;
        const valueName = flds.find(f => f.id === 'value')?.value as string;
        const yearRaw = flds.find(f => f.id === 'year')?.value;
        const mintCityDisplay = flds.find(f => f.id === 'mintCity')?.value as string;
        const description = flds.find(f => f.id === 'description')?.value as string;

        if (!collectionId) {
            errorText = 'Keine Collection ausgewählt.';
            return;
        }
        if (!countryName || !valueName || !yearRaw) {
            errorText = 'Bitte Prägeland, Wert und Jahr ausfüllen.';
            return;
        }

        const countryCode = $coinCountryMap[countryName];
        const value = $stringCoinValuesMap[valueName];
        const year = parseInt(String(yearRaw), 10);

        if (!countryCode) {
            errorText = 'Ausgewähltes Land nicht gefunden.';
            return;
        }
        if (value === undefined) {
            errorText = 'Ausgewählter Wert nicht gefunden.';
            return;
        }
        if (isNaN(year)) {
            errorText = 'Bitte ein gültiges Jahr eingeben.';
            return;
        }

        let mint: string | undefined;
        if (mintCityDisplay) {
            const cityName = mintCityDisplay.split(' (')[0];
            mint = $cityMintMap[cityName];
        }

        const success = await createCoin({
            year,
            value,
            country: countryCode,
            collectionId,
            mint,
            description: description || undefined
        });

        if (success) {
            resetFields();
            close();
        } else {
            errorText = 'Münze konnte nicht erstellt werden.';
        }
    }

    export function show() {
        resetFields();
        dialog.show();
    }

    export function close() {
        resetFields();
        dialog.close();
    }
</script>

<Dialog
    bind:this={dialog}
    title="Neue Münze hinzufügen"
    description={`Fügen Sie eine neue Münze zu ${currentCollection?.name ?? ""} hinzu`}
    {fields}
    buttonText="Münze hinzufügen"
    {onSubmit}
    {errorText}
/>
