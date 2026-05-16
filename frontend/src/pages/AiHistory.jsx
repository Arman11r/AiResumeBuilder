import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import NotificationDropdown from '../components/NotificationDropdown';

const TYPE_LABELS = {
    SUMMARY:      { label: 'Summary',        color: '#3b82f6', bg: '#eff6ff' },
    BULLETS:      { label: 'Bullet Points',  color: '#8b5cf6', bg: '#f5f3ff' },
    COVER_LETTER: { label: 'Cover Letter',   color: '#ec4899', bg: '#fdf2f8' },
    IMPROVE:      { label: 'Improve',        color: '#f59e0b', bg: '#fffbeb' },
    ATS:          { label: 'ATS Check',      color: '#10b981', bg: '#ecfdf5' },
    SKILLS:       { label: 'Skills',         color: '#06b6d4', bg: '#ecfeff' },
    TRANSLATE:    { label: 'Translate',      color: '#64748b', bg: '#f8fafc' },
    TAILOR:       { label: 'Tailor',         color: '#f97316', bg: '#fff7ed' },
};

export default function AiHistory() {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expanded, setExpanded] = useState(null);
    const [filter, setFilter] = useState('ALL');

    useEffect(() => {
        const fetchHistory = async () => {
            setLoading(true);
            try {
                const res = await api.get(`/ai/history/${user.userId}`);
                setHistory(res.data || []);
            } catch (err) {
                // silently handle
            } finally {
                setLoading(false);
            }
        };
        fetchHistory();
    }, [user.userId]);

    const filtered = filter === 'ALL'
        ? history
        : history.filter(item => item.requestType === filter);

    const uniqueTypes = ['ALL', ...new Set(history.map(h => h.requestType).filter(Boolean))];

    const formatDate = (dateStr) => {
        if (!dateStr) return '—';
        try {
            return new Date(dateStr).toLocaleString('en-US', {
                month: 'short', day: 'numeric', year: 'numeric',
                hour: '2-digit', minute: '2-digit',
            });
        } catch { return '—'; }
    };

    const totalTokens = history.reduce((acc, h) => acc + (h.tokensUsed || 0), 0);

    return (
        <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
            <nav className="navbar">
                <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
                    Resume<span>AI</span>
                </div>
                <div className="nav-links">
                    <button className="nav-link" onClick={() => navigate('/dashboard')}>← Dashboard</button>
                    <div className="nav-divider" />
                    <NotificationDropdown userId={user.userId} />
                </div>
            </nav>

            <div className="dashboard-container fade-in">
                {/* Header */}
                <div style={{ marginBottom: 32 }}>
                    <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--primary)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 10 }}>
                        AI Engine
                    </div>
                    <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em', marginBottom: 8 }}>AI Request History</h2>
                    <p style={{ fontSize: 14, color: 'var(--text-muted)' }}>
                        Browse all previous AI-generated content, ATS checks, cover letters and more.
                    </p>
                </div>

                {/* Stats */}
                {history.length > 0 && (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 32 }}>
                        <div className="stat-card">
                            <div className="stat-label">Total Requests</div>
                            <div className="stat-value">{history.length}</div>
                            <div className="stat-sub">all time</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">Tokens Used</div>
                            <div className="stat-value">{totalTokens.toLocaleString()}</div>
                            <div className="stat-sub">estimated</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">AI Model</div>
                            <div className="stat-value" style={{ fontSize: 20 }}>Gemini</div>
                            <div className="stat-sub">Google AI</div>
                        </div>
                    </div>
                )}

                {/* Type filter */}
                {history.length > 0 && (
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
                        {uniqueTypes.map(type => {
                            const meta = TYPE_LABELS[type] || { label: type, color: '#6b7280', bg: '#f3f4f6' };
                            const isActive = filter === type;
                            return (
                                <button
                                    key={type}
                                    onClick={() => setFilter(type)}
                                    style={{
                                        padding: '5px 14px',
                                        borderRadius: 999,
                                        border: `1.5px solid ${isActive ? meta.color : 'var(--border)'}`,
                                        background: isActive ? meta.bg : 'var(--surface)',
                                        color: isActive ? meta.color : 'var(--text-muted)',
                                        fontSize: 12,
                                        fontWeight: 600,
                                        cursor: 'pointer',
                                        transition: 'all 0.15s',
                                        fontFamily: 'inherit',
                                    }}
                                >
                                    {type === 'ALL' ? `All (${history.length})` : (meta.label || type)}
                                </button>
                            );
                        })}
                    </div>
                )}

                {/* Table */}
                <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                    {loading ? (
                        <div style={{ textAlign: 'center', padding: '64px 0' }}>
                            <div className="spinner spinner-dark" style={{ margin: '0 auto 12px' }} />
                            <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>Loading history…</p>
                        </div>
                    ) : filtered.length === 0 ? (
                        <div className="empty-state">
                            <h3>No AI requests yet</h3>
                            <p>Generate a summary, check ATS, or create a cover letter to see history here.</p>
                            <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>Go to Dashboard</button>
                        </div>
                    ) : (
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                            <thead>
                                <tr style={{ background: '#f8fafc', borderBottom: '1px solid var(--border)' }}>
                                    {['Date & Time', 'Type', 'Preview', 'Tokens', 'Status'].map(h => (
                                        <th key={h} style={{
                                            padding: '11px 16px', textAlign: 'left',
                                            fontSize: 11, fontWeight: 700, color: '#6b7280',
                                            textTransform: 'uppercase', letterSpacing: '0.05em',
                                        }}>{h}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map(item => {
                                    const meta = TYPE_LABELS[item.requestType] || { label: item.requestType, color: '#6b7280', bg: '#f3f4f6' };
                                    const isOpen = expanded === item.requestId;
                                    return (
                                        <React.Fragment key={item.requestId}>
                                            <tr
                                                style={{
                                                    borderBottom: '1px solid var(--border)',
                                                    cursor: 'pointer',
                                                    background: isOpen ? 'var(--bg)' : 'transparent',
                                                    transition: 'background 0.15s',
                                                }}
                                                onClick={() => setExpanded(isOpen ? null : item.requestId)}
                                            >
                                                <td style={{ padding: '13px 16px', color: 'var(--text-muted)' }}>
                                                    {formatDate(item.createdAt)}
                                                </td>
                                                <td style={{ padding: '13px 16px' }}>
                                                    <span style={{
                                                        background: meta.bg,
                                                        color: meta.color,
                                                        padding: '3px 10px',
                                                        borderRadius: 999,
                                                        fontSize: 11,
                                                        fontWeight: 700,
                                                    }}>
                                                        {meta.label || item.requestType}
                                                    </span>
                                                </td>
                                                <td style={{ padding: '13px 16px', maxWidth: 280 }}>
                                                    <span style={{
                                                        color: 'var(--text-secondary)',
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                        display: 'block',
                                                    }}>
                                                        {item.content
                                                            ? item.content.slice(0, 100) + (item.content.length > 100 ? '…' : '')
                                                            : <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>No content</span>
                                                        }
                                                    </span>
                                                </td>
                                                <td style={{ padding: '13px 16px', color: 'var(--text-muted)' }}>
                                                    {item.tokensUsed ? item.tokensUsed.toLocaleString() : '—'}
                                                </td>
                                                <td style={{ padding: '13px 16px' }}>
                                                    <span style={{
                                                        background: item.status === 'COMPLETED' ? '#d1fae5' : '#fee2e2',
                                                        color: item.status === 'COMPLETED' ? '#065f46' : '#991b1b',
                                                        padding: '2px 8px',
                                                        borderRadius: 4,
                                                        fontSize: 11,
                                                        fontWeight: 700,
                                                    }}>
                                                        {item.status === 'COMPLETED' ? 'SUCCESS' : (item.status || 'UNKNOWN')}
                                                    </span>
                                                </td>
                                            </tr>
                                            {isOpen && item.content && (
                                                <tr style={{ background: 'var(--bg)', borderBottom: '1px solid var(--border)' }}>
                                                    <td colSpan={5} style={{ padding: '16px 20px' }}>
                                                        <div style={{
                                                            background: 'var(--surface)',
                                                            border: '1px solid var(--border)',
                                                            borderRadius: 8,
                                                            padding: '14px 16px',
                                                            fontSize: 13,
                                                            lineHeight: 1.7,
                                                            color: 'var(--text-secondary)',
                                                            whiteSpace: 'pre-wrap',
                                                            maxHeight: 240,
                                                            overflowY: 'auto',
                                                        }}>
                                                            {item.content}
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </React.Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
