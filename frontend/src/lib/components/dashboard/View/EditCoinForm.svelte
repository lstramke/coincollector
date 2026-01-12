<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { cityMintMap, mintCityMap } from "$lib/stores/mintCity.store";
    import type { DialogField } from "$lib/types/dialogField";
    import { coinValuesStringMap, stringCoinValuesMap } from "$lib/stores/coinValues.store";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import { updateCoin } from "$lib/stores/coin.store";
    import type { Coin } from "$lib/types/coin";

    let dialog: Dialog;
    let errorText = $state<string | null>(null);

    let coin = $state<Coin | undefined>(undefined);

    let fields: DialogField[] = $derived.by(() => {
        if (!coin) return [];
        const countryName = Object.entries($coinCountryMap).find(([_, code]) => code === coin?.country)?.[0] || "";
        return [
            {
                id: "country",
                label: "Prägeland*",
                value: countryName,
                type: "select",
                required: true,
                options: Object.entries($coinCountryMap).map(([name, code]) => name).sort()
            },
            {
                id: "value",
                label: "Wert*",
                value: $coinValuesStringMap[coin.value] ?? "",
                type: "select",
                required: true,
                options: Object.values($coinValuesStringMap)
            },
            {
                id: "year",
                label: "Prägejahr*",
                value: coin.year,
                type: "number",
                required: true,
                placeholder: "Jahr eingeben"
            },
            {
                id: "mintCity",
                label: "Prägestadt",
                value: coin.mint
                    ? `${$mintCityMap[coin.mint]} (${coin.mint})`
                    : "",
                type: "select",
                options: Object.entries($cityMintMap).map(([name, code]) => `${name} (${code})`),
                placeholder: "Prägestadt wählen",
                description: "Aktuell nur für Deutschland unterstützt"
            },
            {
                id: "description",
                label: "Beschreibung",
                value: coin.description,
                type: "text",
                placeholder: "",
                description: "Hier können Sie die Beschreibung der Münze anpassen."
            }
        ];
    });

    function resetFields() {
        errorText = null;
    }

    async function onSubmit(flds: DialogField[]) {
        if (!coin) return;

        errorText = null;

        const countryName = flds.find(f => f.id === 'country')?.value as string;
        const valueName = flds.find(f => f.id === 'value')?.value as string;
        const yearRaw = flds.find(f => f.id === 'year')?.value;
        const mintCityDisplay = flds.find(f => f.id === 'mintCity')?.value as string;
        const description = flds.find(f => f.id === 'description')?.value as string;

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

        const success = await updateCoin(coin.id, {
            year,
            value,
            country: countryCode,
            mint,
            collectionId: coin.collectionId,
            description: description !== coin.description ? description : undefined
        });

        if (success) {
            resetFields();
            close();
        } else {
            errorText = 'Münze konnte nicht aktualisiert werden.';
        }
    }

    export function show(coinToEdit: Coin) {
        coin = coinToEdit;
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
    title="Münze bearbeiten"
    description="Bearbeiten Sie die Daten dieser Münze."
    {fields}
    buttonText="Änderungen speichern"
    {onSubmit}
    {errorText}
/>
