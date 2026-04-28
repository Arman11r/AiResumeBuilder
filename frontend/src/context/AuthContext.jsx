import React, { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        const userId = localStorage.getItem('userId');
        const email = localStorage.getItem('email');
        const role = localStorage.getItem('role');
        if (token && userId) {
            setUser({ token, userId, email, role });
        }
        setLoading(false);
    }, []);

    const login = async (email, password) => {
        const res = await api.post('/auth/login', { email, password });
        const { token, userId } = res.data;
        localStorage.setItem('token', token);
        localStorage.setItem('userId', userId);
        localStorage.setItem('email', email);
        
        // Fetch role
        const profileRes = await api.get('/auth/profile');
        const role = profileRes.data.role || 'USER';
        localStorage.setItem('role', role);
        
        setUser({ token, userId, email, role });
    };

    const register = async (userData) => {
        const res = await api.post('/auth/register', userData);
        const { token, userId, email } = res.data;
        localStorage.setItem('token', token);
        localStorage.setItem('userId', userId);
        localStorage.setItem('email', email);
        localStorage.setItem('role', 'USER');
        setUser({ token, userId, email, role: 'USER' });
    };

    const logout = () => {
        localStorage.clear();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, register, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};
