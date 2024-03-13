import axios from 'axios';

// const BASE_URL = '/';
const BASE_URL = 'http://localhost:8080';
export const axiosInstance = axios.create({ baseURL: BASE_URL });
