import React, { useState, useContext, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Profile() {
    const { user, logout } = useContext(AuthContext);
    const { showToast } = useToast();
    const navigate = useNavigate();
    
    const [profile, setProfile] = useState(null);
    const [fullName, setFullName] = useState('');
    const [password, setPassword] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');

    useEffect(() => {
        api.get('/auth/profile').then(res => {
            setProfile(res.data);
            setFullName(res.data.fullName);
        }).catch(() => showToast('Failed to load profile', 'error'));
    }, []);

    const handleUpdateProfile = async () => {
        try {
            await api.put('/auth/profile', { fullName, phone: profile.phone });
            showToast('Profile updated', 'success');
        } catch (err) { showToast('Update failed', 'error'); }
    };

    const handleChangePassword = async () => {
        if (!currentPassword || !password) return showToast('Please fill in both password fields', 'error');
        try {
            await api.put('/auth/password', { currentPassword, newPassword: password });
            showToast('Password updated', 'success');
            setPassword('');
            setCurrentPassword('');
        } catch (err) { showToast(err.response?.data?.message || 'Password change failed', 'error'); }
    };

    const handleUpgrade = async () => {
        try {
            await api.put('/auth/subscription', { plan: 'PREMIUM' });
            showToast('Upgraded to PREMIUM plan!', 'success');
            setTimeout(() => {
                logout(); // Logout to refresh claims
            }, 2000);
        } catch (err) { showToast('Upgrade failed', 'error'); }
    };

    if (!profile) return <div>Loading...</div>;

    return (
        <div>
            <nav className="navbar">
                <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>← Dashboard</div>
            </nav>
            <div className="dashboard-container fade-in" style={{ maxWidth: 600 }}>
                <h2>My Profile</h2>
                
                <div className="card">
                    <h3>Account Info</h3>
                    <div className="form-group">
                        <label>Email (Read Only)</label>
                        <input className="input-field" value={profile.email} disabled style={{ background: '#f8fafc' }} />
                    </div>
                    <div className="form-group">
                        <label>Full Name</label>
                        <input className="input-field" value={fullName} onChange={e => setFullName(e.target.value)} />
                    </div>
                    <button className="btn btn-primary" onClick={handleUpdateProfile}>Update Profile</button>
                </div>

                <div className="card">
                    <h3>Subscription Plan</h3>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                            <span style={{ fontSize: 18, fontWeight: 700, color: profile.subscriptionPlan === 'PREMIUM' ? 'var(--primary)' : 'var(--text-main)' }}>
                                {profile.subscriptionPlan} PLAN
                            </span>
                            <p style={{ margin: '5px 0 0 0', fontSize: 13 }}>{profile.subscriptionPlan === 'FREE' ? 'Upgrade for unlimited AI calls and exports.' : 'You have access to all premium features!'}</p>
                        </div>
                        <div style={{ display: 'flex', gap: 10 }}>
                            <button className="btn btn-outline" onClick={() => navigate('/ai-history')}>View AI History</button>
                            {profile.subscriptionPlan === 'FREE' && (
                                <button className="btn btn-ai" onClick={handleUpgrade}>Upgrade Now</button>
                            )}
                        </div>
                    </div>
                </div>

                <div className="card">
                    <h3>Security</h3>
                    <div className="form-group">
                        <label>Current Password</label>
                        <input className="input-field" type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} placeholder="Enter current password" />
                    </div>
                    <div className="form-group">
                        <label>New Password</label>
                        <input className="input-field" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 6 characters" />
                    </div>
                    <button className="btn btn-outline" onClick={handleChangePassword}>Change Password</button>
                </div>
            </div>
        </div>
    );
}
