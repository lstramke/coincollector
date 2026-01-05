<script lang="ts">
  import { isAuthenticated } from "$lib/stores/auth.store";
  import AuthTabs from "$lib/components/auth/AuthTabs.svelte";
  import MainLayout from "$lib/components/dashboard/MainLayout.svelte";
</script>

{#if $isAuthenticated}
  <MainLayout />
{:else}
  <AuthTabs />
{/if}


