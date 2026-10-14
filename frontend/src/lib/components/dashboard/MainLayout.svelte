<script lang="ts">
    import Sidebar from './Sidebar/Sidebar.svelte';
    import View from './View/View.svelte';
    import { groupState } from '$lib/stores/group.store.svelte';
        
</script>

<div class="grid min-h-screen grid-cols-[1.1fr_4fr]">
    <Sidebar groups={groupState.groups} />
    <View />
</div>