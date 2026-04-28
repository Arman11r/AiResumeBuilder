import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function AiHistory() {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [history, setHistory] = useState([]);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const res = await api.get(`/ai/history/${user.userId}`);
                setHistory(res.data || []);
            } catch (err) {}
        };
        fetchHistory();
    }, [user.userId]);

    return (
        <div>
            <nav className="navbar">
                <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>← Dashboard</div>
                <div style={{ fontWeight: 600 }}>AI Request History</div>
            </nav>
            <div className="dashboard-container fade-in">
                <h2>AI History Log</h2>
                <p className="text-muted">Review all your previous AI-generated content, ATS checks, and Cover Letters.</p>

                <div className="card">
                    {history.length === 0 ? (
                        <p style={{ color: 'var(--text-muted)' }}>No AI requests made yet.</p>
                    ) : (
                        <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ borderBottom: '2px solid var(--border)' }}>
                                    <th style={{ padding: 12 }}>Date</th>
                                    <th style={{ padding: 12 }}>Operation Type</th>
                                    <th style={{ padding: 12 }}>Tokens Used</th>
                                    <th style={{ padding: 12 }}>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map(item => (
                                    <tr key={item.requestId} style={{ borderBottom: '1px solid var(--border)' }}>
                                        <td style={{ padding: 12 }}>{new Date(item.timestamp).toLocaleString()}</td>
                                        <td style={{ padding: 12 }}>{item.operationType}</td>
                                        <td style={{ padding: 12 }}>{item.tokensUsed}</td>
                                        <td style={{ padding: 12 }}>
                                            <span style={{ background: item.status === 'SUCCESS' ? '#d1fae5' : '#fee2e2', color: item.status === 'SUCCESS' ? '#065f46' : '#991b1b', padding: '2px 8px', borderRadius: 4, fontSize: 12, fontWeight: 700 }}>
                                                {item.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
