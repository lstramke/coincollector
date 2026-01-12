import axios from 'axios';

/**
 * Configured Axios instance for API requests
 * 
 * This API client is pre-configured with:
 * - Base URL from environment variable VITE_API_BASE_URL
 * - JSON content type header
 * - 10 second timeout for all requests
 * 
 * @example
 * import api from '$lib/api/api';
 * const response = await api.get('/endpoint');
 */
const api = axios.create({
  baseURL: "/api",
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

export default api;