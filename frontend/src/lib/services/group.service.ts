import api from '$lib/api/api';
import type { Group } from '$lib/types/group';

export type CreateGroupRequest = {
	name: string;
	collections?: string[];
};

export type GroupMetadata = {
	name: string;
};

const API_BASE = '/groups';

export const groupService = {
	async getAllGroups(): Promise<Group[]> {
		  const res = await api.get<Group[]>(API_BASE);
		  return res.data;
	},
	async getGroup(id: string): Promise<Group> {
		  const res = await api.get<Group>(`${API_BASE}/${id}`);
		  return res.data;
	},
	async createGroup(data: CreateGroupRequest): Promise<Group> {
		  const res = await api.post<Group>(API_BASE, data);
		  return res.data;
	},
	async updateGroup(id: string, data: GroupMetadata): Promise<GroupMetadata> {
		  const res = await api.patch<GroupMetadata>(`${API_BASE}/${id}`, data);
		  return res.data;
	},
	async deleteGroup(id: string): Promise<void> {
		  await api.delete(`${API_BASE}/${id}`);
	}
};
