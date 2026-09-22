<script lang="ts">

    import type { AuthField } from "$lib/types/authField";
    import eyeClosedIcon from '../../../assets/icons/eye-closed.svg';
    import eyeOpenIcon from '../../../assets/icons/eye-open.svg';

    const { title, description, submitLabel, fields, onSubmit } = $props<{
        title: string;
        description: string;
        submitLabel: string;
        fields: AuthField[];
        onSubmit: (fields: AuthField[]) => void;
    }>();

    let visibleFields = $state<Record<string, boolean>>({});

    function toggleVisibility(fieldId: string) {
        visibleFields = {
            ...visibleFields,
            [fieldId]: !visibleFields[fieldId]
        };
    }

    function handleSubmit(event: Event) {
        event.preventDefault();
        onSubmit(fields);
    }
</script>

<form
    class="space-y-4 rounded-3xl bg-white px-7 py-8 border border-[#f3e7c9] shadow-[0_20px_55px_rgba(212,160,23,0.16)]"
    onsubmit={handleSubmit}
>
    <h3 class="text-lg font-semibold text-[var(--text-primary)]">{title}</h3>
    <p class="text-sm text-[var(--text-secondary)] mb-4">{description}</p>
    {#each fields as field (field.id)}
        <div>
            <label class="block text-sm font-medium mb-2 text-[var(--text-primary)]" for={field.id}>
                {field.label}
            </label>
            <div class="relative">
                <input
                    id={field.id}
                    type={field.addVisibilityToggle && visibleFields[field.id] ? 'text' : field.type ?? 'text'}
                    bind:value={field.value}
                    placeholder={field.placeholder}
                    required={field.required}
                    class:pr-12={field.addVisibilityToggle}
                    class="w-full px-4 py-2.5 border border-[var(--color-primary)] rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent bg-white text-[var(--text-primary)]"
                />
                {#if field.addVisibilityToggle}
                    <button
                        type="button"
                        class="absolute inset-y-0 right-0 flex w-12 items-center justify-center text-[var(--text-secondary)] hover:text-[var(--color-primary)]"
                        aria-label={visibleFields[field.id] ? 'Passwort verbergen' : 'Passwort anzeigen'}
                        title={visibleFields[field.id] ? 'Passwort verbergen' : 'Passwort anzeigen'}
                        onclick={() => toggleVisibility(field.id)}
                    >
                        <img
                            src={visibleFields[field.id] ? eyeClosedIcon : eyeOpenIcon}
                            alt=""
                            class="h-5 w-5"
                        />
                    </button>
                {/if}
            </div>
        </div>
    {/each}
    <button
        type="submit"
        class="w-full rounded-lg py-3 text-sm font-semibold transition-all duration-200 bg-[var(--color-primary)] text-white shadow-[0_8px_16px_rgba(212,160,23,0.25)] hover:brightness-105"
    >
        {submitLabel}
    </button>
</form>