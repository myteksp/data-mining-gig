import axios from "axios";

const BASE_URL = "/";
export const axiosInstance = axios.create({ baseURL: BASE_URL });
