<script lang="ts">
    const { title = 'CoinCollector' } = $props<{ title?: string }>();
</script>

<header class="flex items-center gap-3 border-b border-[var(--border-transparent)] px-5 py-6 text-[var(--text-white)]">
    <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--color-primary)]">
       <svg class="w-9 h-9 block text-[var(--text-white)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" preserveAspectRatio="xMidYMid meet" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" data-slot="icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.25 7.756a4.5 4.5 0 1 0 0 8.488M7.5 10.5h5.25m-5.25 3h5.25M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
       </svg>
     </div>
    <div class="flex flex-col leading-snug flex-1">
        <div class="text-xl font-semibold">{title}</div>
        <p class="text-sm text-[var(--text-white)]/70">Meine Sammlung</p>
    </div>
    <button
        type="button"
        class="ml-auto flex items-center justify-center gap-2 rounded-full bg-[var(--color-primary)] hover:bg-[var(--color-warning)] transition-colors w-auto h-10 px-4"
        aria-label="Logout"
        onclick={() => {}}
    >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-6 h-6 text-[var(--text-white)]" aria-hidden="true" data-slot="icon">
          <path fill-rule="evenodd" d="M2 4.75A2.75 2.75 0 0 1 4.75 2h3a2.75 2.75 0 0 1 2.75 2.75v.5a.75.75 0 0 1-1.5 0v-.5c0-.69-.56-1.25-1.25-1.25h-3c-.69 0-1.25.56-1.25 1.25v6.5c0 .69.56 1.25 1.25 1.25h3c.69 0 1.25-.56 1.25-1.25v-.5a.75.75 0 0 1 1.5 0v.5A2.75 2.75 0 0 1 7.75 14h-3A2.75 2.75 0 0 1 2 11.25v-6.5Zm9.47.47a.75.75 0 0 1 1.06 0l2.25 2.25a.75.75 0 0 1 0 1.06l-2.25 2.25a.75.75 0 1 1-1.06-1.06l.97-.97H5.25a.75.75 0 0 1 0-1.5h7.19l-.97-.97a.75.75 0 0 1 0-1.06Z" clip-rule="evenodd"/>
        </svg>
        Abmelden
    </button>
</header>