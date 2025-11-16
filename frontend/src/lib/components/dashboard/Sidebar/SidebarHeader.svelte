<script lang="ts">
    const { title = 'CoinCollector' } = $props<{ title?: string }>();
</script>

<header class="flex items-center gap-3 border-b border-[var(--border-transparent)] px-5 py-6 text-[var(--text-white)]">
    <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--color-primary)]"></div>
    <div class="flex flex-col leading-snug">
        <div class="text-xl font-semibold">{title}</div>
        <p class="text-sm text-[var(--text-white)]/70">Meine Sammlung</p>
    </div>
</header>