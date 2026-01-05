<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { cityMintMap, mintCityMap } from "$lib/stores/mintCity.store";
    import type { DialogField } from "$lib/types/dialogField";
    import { coinValuesStringMap } from "$lib/stores/coinValues.store";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";
    import type { Coin } from "$lib/types/coin";

    let dialog: Dialog;

    let coin = $state<Coin | undefined>(undefined);

    let fields: DialogField[] = $derived.by(() => {
        if (!coin) return [];
        return [
            {
                id: "country",
                label: "Prägeland*",
                value: coin.country,
                type: "select",
                required: true,
                options: Object.entries($coinCountryMap).map(([name, code]) => name)
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
                value: coin.mintCity
                    ? `${$mintCityMap[coin.mintCity]} (${coin.mintCity})`
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

    function onSubmit(fields: DialogField[]) {
        console.log(fields);
    }

    export function show(coinToEdit: Coin) {
        coin = coinToEdit;
        dialog.show();
    }

    export function close() {
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
/>
