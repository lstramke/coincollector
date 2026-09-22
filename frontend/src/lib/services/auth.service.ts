import api from '$lib/api/api';

type LoginRequest = {
  username: string;
  password: string;
};

type RegistrationRequest = {
  username: string;
  password: string;
};

type SetupPasswordRequest = {
  username: string;
  newPassword: string;
};

export const authService = {
  async login(data: LoginRequest): Promise<void> {
    await api.post('/login', data);
  },

  async register(data: RegistrationRequest): Promise<void> {
    await api.post('/registration', data);
  },

  async logout(): Promise<void> {
    await api.post('/logout');
  },

  async setupPassword(data: SetupPasswordRequest): Promise<void> {
    await api.post('/password/setup', data);
  }
};