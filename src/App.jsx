import React, { useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthContext } from './context/AuthContext';
import Home from './pages/Home';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ResumeBuilder from './pages/ResumeBuilder';
import JobMatch from './pages/JobMatch';
import CoverLetter from './pages/CoverLetter';
import Profile from './pages/Profile';
import Admin from './pages/Admin';
import AiHistory from './pages/AiHistory';
import PublicGallery from './pages/PublicGallery';

const PrivateRoute = ({ children }) => {
    const { user, loading } = useContext(AuthContext);
    if (loading) return <div>Loading...</div>;
    return user ? children : <Navigate to="/login" />;
};

const AdminRoute = ({ children }) => {
    const { user, loading } = useContext(AuthContext);
    if (loading) return <div>Loading...</div>;
    return (user && user.role === 'ADMIN') ? children : <Navigate to="/dashboard" />;
};

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/builder/:id" element={<PrivateRoute><ResumeBuilder /></PrivateRoute>} />
                <Route path="/job-match" element={<PrivateRoute><JobMatch /></PrivateRoute>} />
                <Route path="/cover-letter/:resumeId?" element={<PrivateRoute><CoverLetter /></PrivateRoute>} />
                <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
                <Route path="/ai-history" element={<PrivateRoute><AiHistory /></PrivateRoute>} />
                <Route path="/admin" element={<AdminRoute><Admin /></AdminRoute>} />
                <Route path="/gallery" element={<PublicGallery />} />
            </Routes>
        </BrowserRouter>
    );
}