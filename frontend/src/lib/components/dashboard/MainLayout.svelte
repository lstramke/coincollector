<script lang="ts">
    import Sidebar from './Sidebar/Sidebar.svelte';
    import type { Group } from '$lib/types/group';
    import View from './View/View.svelte';

    const groups: Group[] = [
        {
            id: 'g-de',
            name: 'Deutsche Euromünzen',
            collections: [
                {
                    id: 'c-de-2015',
                    name: 'Deutschland 2015',
                    coins: [
                        { id: 'coin-de-2015-2e', value: 2, country: 'Deutschland', year: 2015, mintCity: 'München', description: 'Gedenkmünze Deutsche Einheit' },
                        { id: 'coin-de-2015-1e', value: 1, country: 'Deutschland', year: 2015, mintCity: 'Berlin', description: 'Bundesadler' }
                    ]
                },
                {
                    id: 'c-de-2020',
                    name: 'Deutschland 2020',
                    coins: [
                        { id: 'coin-de-2020-50c', value: 0.5, country: 'Deutschland', year: 2020, mintCity: null, description: 'Brandenburger Tor' }
                    ]
                }
            ]
        },
        {
            id: 'g-fr',
            name: 'Französische Euromünzen',
            collections: [
                {
                    id: 'c-fr-2020',
                    name: 'Frankreich 2020',
                    coins: [
                        { id: 'coin-fr-2020-2e', value: 2, country: 'Frankreich', year: 2020, mintCity: 'Paris', description: 'Marianne' }
                    ]
                }
            ]
        }
    ];
</script>

<div class="grid min-h-screen grid-cols-[1.1fr_4fr]">
    <Sidebar {groups} />
    <View />
</div>