import api from '$lib/api/api';
import type { Collection } from '$lib/types/collection';

export type CreateCollectionRequest = {
    name: string;
	groupId: string;
	coins?: string[];
};

export type CollectionMetadata = {
    name: string;
    groupId: string;
};

const API_BASE = '/collections';

export const collectionService = {
	async getCollection(id: string): Promise<Collection> {
		const response = await api.get(`${API_BASE}/${id}`);
		return response.data;
	},
	async createCollection(data: CreateCollectionRequest): Promise<Collection> {
		const response = await api.post(API_BASE, data);
		return response.data;
	},
	async updateCollection(id: string, data: CollectionMetadata): Promise<Collection> {
		const response = await api.patch(`${API_BASE}/${id}`, data);
		return response.data;
	},
	async deleteCollection(id: string): Promise<void> {
		await api.delete(`${API_BASE}/${id}`);
	}
};

