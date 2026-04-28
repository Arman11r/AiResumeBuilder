import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080',
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    const userId = localStorage.getItem('userId');
    if (userId) {
        config.headers['X-User-Id'] = userId;
    }
    return config;
});

export default api;
