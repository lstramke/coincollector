import api from '$lib/api/api';

export const systemService = {
	async shutdown(): Promise<void> {
		await api.post('/shutdown');
		window.close();
	}
};
