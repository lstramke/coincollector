<script lang="ts">
    const { title = 'CoinCollector' } = $props<{ title?: string }>();
</script>

<header class="flex items-center gap-3 border-b border-[var(--border-transparent)] px-5 py-6 text-[var(--text-white)]">
    <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--color-primary)]">
       <svg class="w-9 h-9 block text-[var(--text-white)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" preserveAspectRatio="xMidYMid meet" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" data-slot="icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.25 7.756a4.5 4.5 0 1 0 0 8.488M7.5 10.5h5.25m-5.25 3h5.25M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
       </svg>
     </div>
    <div class="flex flex-col leading-snug">
        <div class="text-xl font-semibold">{title}</div>
        <p class="text-sm text-[var(--text-white)]/70">Meine Sammlung</p>
    </div>
</header>