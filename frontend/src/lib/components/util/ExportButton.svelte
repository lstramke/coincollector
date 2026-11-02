
<script lang="ts">
    import { exportService } from "$lib/services/export.service";

    let fileType = $state("csv");

    let exportUrl = $derived(
        exportService.getExportUrl({ fileType })
    );
</script>

<div class="flex flex-wrap items-center gap-3 pt-3 text-sm text-[var(--text-white)]">
    <a
        href={exportUrl}
        class="inline-flex items-center justify-center gap-2 rounded-lg border border-[var(--color-primary)] bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-[var(--text-primary)] transition hover:brightness-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary)]"
    >
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" />
        </svg>
        Exportieren
    </a>
    <select
        id="fileType"
        bind:value={fileType}
        class="cursor-pointer appearance-none rounded-lg border border-[var(--border-transparent)] bg-[var(--btn-background)] px-3 py-2 text-sm font-medium text-[var(--text-white)] outline-none transition hover:bg-[var(--btn-hover)] focus-visible:ring-2 focus-visible:ring-[var(--color-primary)]"
    >
        <option value="csv" class="bg-[var(--bg-sidebar)] text-white">
            CSV
        </option>
        <option value="json" class="bg-[var(--bg-sidebar)] text-white">
            JSON
        </option>
    </select>
    <label for="fileType" class="shrink-0 font-medium">
        Dateiformat
    </label>
</div>
