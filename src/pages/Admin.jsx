import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Admin() {
    const { user } = useContext(AuthContext);
    const { showToast } = useToast();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('users');
    const [templates, setTemplates] = useState([]);

    useEffect(() => {
        const loadAdminData = async () => {
            try {
                const tRes = await api.get('/templates');
                setTemplates(tRes.data || []);
            } catch (err) {
                // Ignore error if not authorized
            }
        };
        loadAdminData();
    }, []);

    return (
        <div>
            <nav className="navbar" style={{ background: '#0f172a', color: 'white' }}>
                <div className="logo" style={{ color: 'white', cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>← Return to Dashboard</div>
                <div style={{ fontWeight: 700 }}>ResumeAI Admin Portal</div>
            </nav>
            <div className="builder-layout">
                {/* Admin Sidebar */}
                <div className="builder-sidebar" style={{ width: 250, background: '#1e293b', color: 'white', borderRight: 'none' }}>
                    <div className="sidebar-nav">
                        <div className="nav-item" style={{ background: activeTab === 'users' ? '#334155' : 'transparent', color: 'white' }} onClick={() => setActiveTab('users')}>
                            👥 User Management
                        </div>
                        <div className="nav-item" style={{ background: activeTab === 'templates' ? '#334155' : 'transparent', color: 'white' }} onClick={() => setActiveTab('templates')}>
                            📄 Template Editor
                        </div>
                        <div className="nav-item" style={{ background: activeTab === 'analytics' ? '#334155' : 'transparent', color: 'white' }} onClick={() => setActiveTab('analytics')}>
                            📈 Platform Analytics
                        </div>
                        <div className="nav-item" style={{ background: activeTab === 'ai' ? '#334155' : 'transparent', color: 'white' }} onClick={() => setActiveTab('ai')}>
                            🤖 AI Usage Stats
                        </div>
                    </div>
                </div>

                {/* Admin Content */}
                <div className="builder-editor" style={{ flex: 1, padding: 40, background: '#f8fafc' }}>
                    
                    {activeTab === 'users' && (
                        <div className="fade-in">
                            <h2>User Management</h2>
                            <p className="text-muted">Manage all registered accounts, roles, and subscriptions.</p>
                            <div className="card">
                                <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr style={{ borderBottom: '2px solid var(--border)' }}>
                                            <th style={{ padding: 12 }}>User ID</th>
                                            <th style={{ padding: 12 }}>Email</th>
                                            <th style={{ padding: 12 }}>Role</th>
                                            <th style={{ padding: 12 }}>Plan</th>
                                            <th style={{ padding: 12 }}>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr style={{ borderBottom: '1px solid var(--border)' }}>
                                            <td style={{ padding: 12 }}>usr_10293</td>
                                            <td style={{ padding: 12 }}>testdev@resumeai.com</td>
                                            <td style={{ padding: 12 }}>USER</td>
                                            <td style={{ padding: 12 }}><span style={{ background: '#d1fae5', color: '#065f46', padding: '2px 8px', borderRadius: 4, fontSize: 12, fontWeight: 700 }}>FREE</span></td>
                                            <td style={{ padding: 12 }}>
                                                <button className="btn btn-outline" style={{ fontSize: 12, padding: '4px 8px' }}>Manage</button>
                                            </td>
                                        </tr>
                                        <tr style={{ borderBottom: '1px solid var(--border)' }}>
                                            <td style={{ padding: 12 }}>usr_99124</td>
                                            <td style={{ padding: 12 }}>pro@resumeai.com</td>
                                            <td style={{ padding: 12 }}>USER</td>
                                            <td style={{ padding: 12 }}><span style={{ background: '#fef3c7', color: '#d97706', padding: '2px 8px', borderRadius: 4, fontSize: 12, fontWeight: 700 }}>PREMIUM</span></td>
                                            <td style={{ padding: 12 }}>
                                                <button className="btn btn-outline" style={{ fontSize: 12, padding: '4px 8px' }}>Manage</button>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}

                    {activeTab === 'templates' && (
                        <div className="fade-in">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                                <div>
                                    <h2>Template Management</h2>
                                    <p className="text-muted">Create, edit, and deactivate system resume templates.</p>
                                </div>
                                <button className="btn btn-primary">+ New Template</button>
                            </div>
                            <div className="resume-grid">
                                {templates.map(t => (
                                    <div key={t.templateId} className="resume-card" style={{ padding: 20 }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <h3 style={{ margin: 0 }}>{t.name}</h3>
                                            <span style={{ background: t.isPremium ? '#fef3c7' : '#d1fae5', color: t.isPremium ? '#d97706' : '#059669', padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 700 }}>
                                                {t.isPremium ? 'PREMIUM' : 'FREE'}
                                            </span>
                                        </div>
                                        <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '10px 0' }}>Category: {t.category}</p>
                                        <p style={{ fontSize: 13, margin: '0 0 15px 0' }}>Usage Count: {t.usageCount}</p>
                                        <div style={{ display: 'flex', gap: 10 }}>
                                            <button className="btn btn-outline" style={{ flex: 1, padding: 6, fontSize: 12 }}>Edit HTML/CSS</button>
                                            <button className="btn btn-danger" style={{ padding: 6, fontSize: 12 }}>Deactivate</button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {activeTab === 'analytics' && (
                        <div className="fade-in">
                            <h2>Platform Analytics</h2>
                            <div style={{ display: 'flex', gap: 20, marginTop: 20 }}>
                                <div className="card" style={{ flex: 1, textAlign: 'center' }}>
                                    <div style={{ fontSize: 40, fontWeight: 800, color: 'var(--primary)' }}>1,248</div>
                                    <div style={{ color: 'var(--text-muted)' }}>Total Users</div>
                                </div>
                                <div className="card" style={{ flex: 1, textAlign: 'center' }}>
                                    <div style={{ fontSize: 40, fontWeight: 800, color: 'var(--success)' }}>5,932</div>
                                    <div style={{ color: 'var(--text-muted)' }}>Resumes Created</div>
                                </div>
                                <div className="card" style={{ flex: 1, textAlign: 'center' }}>
                                    <div style={{ fontSize: 40, fontWeight: 800, color: '#8b5cf6' }}>8,102</div>
                                    <div style={{ color: 'var(--text-muted)' }}>PDF Exports</div>
                                </div>
                            </div>
                        </div>
                    )}

                    {activeTab === 'ai' && (
                        <div className="fade-in">
                            <h2>AI Usage Statistics</h2>
                            <div className="card">
                                <h3>API Token Consumption</h3>
                                <p style={{ fontSize: 14 }}><strong>GPT-4o:</strong> 24.5M Tokens used this month ($122.50 estimated cost)</p>
                                <p style={{ fontSize: 14 }}><strong>Claude 3.5 Sonnet:</strong> 1.2M Tokens used this month ($3.60 estimated cost)</p>
                                <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '20px 0' }} />
                                <h3>Quota Utilization</h3>
                                <div style={{ height: 20, background: '#e2e8f0', borderRadius: 10, overflow: 'hidden' }}>
                                    <div style={{ width: '65%', background: 'var(--ai-glow)', height: '100%' }}></div>
                                </div>
                                <p style={{ fontSize: 12, textAlign: 'right', marginTop: 5 }}>65% of monthly OpenAI limit reached</p>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
