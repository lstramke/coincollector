<script lang="ts">
    import Sidebar from './Sidebar/Sidebar.svelte';
    import View from './View/View.svelte';
    import { groups } from '$lib/stores/groupStore';
        
</script>

<div class="grid min-h-screen grid-cols-[1.1fr_4fr]">
    <Sidebar groups={$groups} />
    <View />
</div>