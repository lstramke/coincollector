<script lang="ts">
    import type { DialogField } from "$lib/types/dialogField";

    let {title , description, fields, buttonText, onSubmit, errorText} = $props<{
        title: string;
        description: string;
        fields: DialogField[];
        buttonText: string;
        onSubmit?: (fields: DialogField[]) => void;
        errorText?: string | null;
    }>();

    let isOpen = $state(false);

    export function show() {
        isOpen = true;
    }

    export function close() {
        isOpen = false;
    }

    function handleSubmit(e: Event) {
        e.preventDefault();
        onSubmit?.(fields);
    }
</script>

{#if isOpen}
    <div class="fixed inset-0 bg-[var(--bg-sidebar)]/20 flex items-center justify-center z-50">
        <div class="bg-[var(--bg-main)] rounded-xl px-8 py-6 w-[400px] md:w-[560px] shadow-xl relative">
            <button
                aria-label="Schließen"
                onclick={close}
                class="absolute top-4 right-4 text-[var(--text-primary)] text-xl font-bold flex items-center justify-center w-8 h-8 cursor-pointer"
            >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="24" height="24" aria-hidden="true">
                    <path fill-rule="evenodd" d="M6.293 6.293a1 1 0 011.414 0L10 8.586l2.293-2.293a1 1 0 111.414 1.414L11.414 10l2.293 2.293a1 1 0 01-1.414 1.414L10 11.414l-2.293 2.293a1 1 0 01-1.414-1.414L8.586 10 6.293 7.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
            </button>

            <h2 class="text-lg font-semibold mb-1 text-[var(--text-primary)]">{title}</h2>
            <p class="text-sm text-[var(--text-secondary)] mb-5">{description}</p>

            <form onsubmit={handleSubmit} class="space-y-4">
                {#each fields as field (field.id)}
                    <div>
                        <label for={field.id} class="block mb-1 text-sm font-medium text-[var(--text-primary)]">
                            {field.label}
                        </label>
                        {#if field.type === "select"}
                            <select
                                id={field.id}
                                bind:value={field.value}
                                required={field.required}
                                class="block w-full h-11 px-3 py-2 rounded-lg border border-[var(--border-standard)] bg-[var(--bg-white)] text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] transition"
                            >
                                {#each field.options as option}
                                    <option value={option}>{option}</option>
                                {/each}
                            </select>
                        {:else}
                            <input
                                id={field.id}
                                type={field.type ?? "text"}
                                bind:value={field.value}
                                required={field.required}
                                placeholder={field.placeholder}
                                class="block w-full h-11 px-3 py-2 rounded-lg border border-[var(--border-standard)] bg-[var(--bg-white)] text-[var(--text-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] transition"
                                disabled={field.disabled}
                            />
                        {/if}
                        {#if field.description}
                            <p class="text-xs text-[var(--text-secondary)] mt-1">{field.description}</p>
                        {/if}
                    </div>
                {/each}

                <div class="flex justify-end gap-3 mt-6">
                    <button
                        type="button"
                        onclick={close}
                        class="px-4 py-2 rounded-lg border border-[var(--border-standard)] text-[var(--text-primary)] bg-[var(--bg-white)] hover:bg-[var(--hover-state)] transition"
                    >
                        Abbrechen
                    </button>

                    <button
                        type="submit"
                        class="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-[var(--text-white)] font-semibold hover:brightness-110 transition"
                    >
                        {buttonText}
                    </button>
                </div>
            </form>
            {#if errorText}
                <div class="text-red-600 mt-2">{errorText}</div>
            {/if}
        </div>
    </div>
{/if}
