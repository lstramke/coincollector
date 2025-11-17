<script lang="ts">
  import LoginWindow from './LoginWindow.svelte';
  import RegisterWindow from './RegisterWindow.svelte';
  
  let activeTab: 'login' | 'register' = 'login';
</script>

<div class="min-h-screen bg-[var(--bg-main)] flex items-center justify-center px-4 py-16">
  <div class="w-full max-w-lg space-y-10">
    <header class="text-center space-y-4">
      <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-[var(--color-primary)] shadow-[0_18px_32px_rgba(212,160,23,0.28)]">
        <svg class="w-15 h-15 block text-[var(--text-white)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" preserveAspectRatio="xMidYMid meet" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" data-slot="icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.25 7.756a4.5 4.5 0 1 0 0 8.488M7.5 10.5h5.25m-5.25 3h5.25M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
       </svg>
      </div>
      <div class="space-y-1">
        <h1 class="text-3xl font-semibold text-[var(--text-primary)]">Euromünzen Sammlung</h1>
        <p class="text-sm text-[var(--text-secondary)]">Verwalten Sie Ihre Münzsammlung</p>
      </div>

      <div class="mb-8 flex justify-center">
        <div class="inline-flex items-center gap-2 rounded-full bg-[#FAF0D8] p-1">
          <button
            type="button"
            class={`rounded-full px-6 py-2 text-sm font-semibold transition-all duration-200 ${
              activeTab === 'login'
                ? 'bg-[var(--color-primary)] text-white shadow-[0_6px_14px_rgba(212,160,23,0.35)]'
                : 'text-[var(--text-secondary)] hover:text-[var(--color-primary)]'
            }`}
            onclick={() => (activeTab = 'login')}
          >
            Anmelden
          </button>
          <button
            type="button"
            class={`rounded-full px-6 py-2 text-sm font-semibold transition-all duration-200 ${
              activeTab === 'register'
                ? 'bg-[var(--color-primary)] text-white shadow-[0_6px_14px_rgba(212,160,23,0.35)]'
                : 'text-[var(--text-secondary)] hover:text-[var(--color-primary)]'
            }`}
            onclick={() => (activeTab = 'register')}
          >
            Registrieren
          </button>
        </div>
      </div>
</header>
    {#if activeTab === 'login'}
      <LoginWindow />
    {:else}
      <RegisterWindow />
    {/if}
  </div>
</div>