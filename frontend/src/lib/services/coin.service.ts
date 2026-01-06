import api from '$lib/api/api';
import type { Coin } from '$lib/types/coin';

export type CreateCoinRequest = {
	year: number;
	value: number;
	country: string;
	collectionId: string;
	mint?: string;
	description?: string;
};

export type UpdateCoinRequest = Partial<CreateCoinRequest>;

const API_BASE = '/coins';

export const coinService = {
	async getCoin(id: string): Promise<Coin> {
		const response = await api.get(`${API_BASE}/${id}`);
		return response.data;
	},
	async createCoin(data: CreateCoinRequest): Promise<Coin> {
		const response = await api.post(API_BASE, data);
		return response.data;
	},
	async updateCoin(id: string, data: UpdateCoinRequest): Promise<Coin> {
		const response = await api.patch(`${API_BASE}/${id}`, data);
		return response.data;
	},
	async deleteCoin(id: string): Promise<void> {
		await api.delete(`${API_BASE}/${id}`);
	}
};
