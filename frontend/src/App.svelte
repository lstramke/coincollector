<script lang="ts">
  import { authState } from "$lib/stores/auth.store.svelte";
  import AuthTabs from "$lib/components/auth/AuthTabs.svelte";
  import MainLayout from "$lib/components/dashboard/MainLayout.svelte";
</script>

{#if authState.isAuthenticated}
  <MainLayout />
{:else}
  <AuthTabs />
{/if}


