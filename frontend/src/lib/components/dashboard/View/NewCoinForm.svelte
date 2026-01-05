<script lang="ts">
    import Dialog from "$lib/components/util/Dialog.svelte";
    import { collectionMap } from "$lib/stores/group.store";
    import { selection } from "$lib/stores/selection.store";
    import { cityMintMap } from "$lib/stores/mintCity.store";
    import type { DialogField } from "$lib/types/dialogField";
    import { stringCoinValuesMap } from "$lib/stores/coinValues.store";
    import { coinCountryMap } from "$lib/stores/coinCountry.store";

    let dialog: Dialog;

    const currentCollection = $selection?.id ? $collectionMap[$selection.id] : undefined;

    let fields: DialogField[] = [
        {
            id: "collection",
            label: "Collection*",
            value: currentCollection?.name ?? "",
            type: "text",
            required: true,
            disabled: true
        },
        {
            id: "country",
            label: "Prägeland*",
            value: "",
            type: "select",
            required: true,
            options: Object.entries($coinCountryMap).map(([name, code]) => name)
        },
        {
            id: "value",
            label: "Wert*",
            value: "",
            type: "select",
            required: true,
            options: Object.keys($stringCoinValuesMap)
        },
        {
            id: "year",
            label: "Prägejahr*",
            value: "",
            type: "number",
            required: true,
            placeholder: "Jahr eingeben"
        },
        {
            id: "mintCity",
            label: "Prägestadt",
            value: "",
            type: "select",
            options: Object.entries($cityMintMap).map(([name, code]) => `${name} (${code})`),
            placeholder: "Prägestadt wählen",
            description: "Aktuell nur für Deutschland unterstützt"
        },
        {
            id: "description",
            label: "Beschreibung",
            value: "",
            type: "text",
            placeholder: "",
            description: "Es wird, wenn es keine Eingabe gibt, eine Beschreibung generiert."
        }
    ];

    function onSubmit(fields: DialogField[]) {
        console.log(fields);
    }

    export function show() {
        dialog.show();
    }

    export function close() {
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
/>
