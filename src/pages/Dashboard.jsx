import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { pushNotification } from '../services/notify';
import NotificationDropdown from '../components/NotificationDropdown';
import QuotaWidget from '../components/QuotaWidget';

const FileIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
    <polyline points="14 2 14 8 20 8"/>
  </svg>
);

const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
  </svg>
);

const TrashIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/>
    <path d="M10 11v6"/><path d="M14 11v6"/>
  </svg>
);

const CopyIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
  </svg>
);

export default function Dashboard() {
  const { user, logout } = useContext(AuthContext);
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [resumes, setResumes] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchResumes = async () => {
    try {
      const res = await api.get(`/resumes/user/${user.userId}`);
      setResumes(res.data || []);
    } catch (err) {
      showToast('Failed to fetch resumes', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchResumes(); }, []);

  const handleCreate = async () => {
    try {
      const res = await api.post('/resumes', {
        title: 'Untitled Resume',
        targetJobTitle: 'Software Engineer',
        templateId: 'tmpl-001',
        language: 'en',
      });
      showToast('Resume created successfully!', 'success');
      pushNotification({
        recipientId: user.userId,
        type: 'RESUME_CREATED',
        message: 'Your new resume “Untitled Resume” has been created. Start adding your details!',
        relatedId: res.data.resumeId,
      });
      navigate(`/builder/${res.data.resumeId}`);
    } catch (err) {
      showToast('Error creating resume', 'error');
    }
  };

  const handleDelete = async (e, id) => {
    e.stopPropagation();
    if (!window.confirm('Delete this resume permanently?')) return;
    try {
      const resume = resumes.find(r => r.resumeId === id);
      await api.delete(`/resumes/${id}`);
      showToast('Resume deleted', 'success');
      pushNotification({
        recipientId: user.userId,
        type: 'RESUME_DELETED',
        message: `Resume “${resume?.title || 'Untitled'}” has been permanently deleted.`,
      });
      fetchResumes();
    } catch (err) {
      showToast('Error deleting', 'error');
    }
  };

  const handleDuplicate = async (e, id) => {
    e.stopPropagation();
    try {
      const res = await api.post(`/resumes/${id}/duplicate`);
      showToast('Resume duplicated ✨', 'success');
      pushNotification({
        recipientId: user.userId,
        type: 'RESUME_CREATED',
        message: `A duplicate of your resume has been created as "${res.data?.title || 'Copy'}".`,
        relatedId: res.data?.resumeId,
      });
      fetchResumes();
    } catch (err) {
      showToast('Error duplicating resume', 'error');
    }
  };

  const avgAts = resumes.length
    ? Math.round(resumes.reduce((acc, r) => acc + (r.atsScore || 0), 0) / resumes.length)
    : 0;

  const getScoreColor = (score) => {
    if (score >= 70) return 'var(--success)';
    if (score >= 50) return 'var(--warning)';
    return 'var(--danger)';
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* ── Navbar ── */}
      <nav className="navbar">
        <div className="logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
          Resume<span>AI</span>
        </div>
        <div className="nav-links">
          <button className="nav-link" onClick={() => navigate('/job-match')}>Job Match</button>
          <button className="nav-link" onClick={() => navigate('/cover-letter')}>Cover Letters</button>
          <button className="nav-link" onClick={() => navigate('/gallery')}>🌐 Gallery</button>
          <button className="nav-link" onClick={() => navigate('/ai-history')}>AI History</button>
          <div className="nav-divider"></div>
          <NotificationDropdown userId={user.userId} />
          <button className="nav-link" onClick={() => navigate('/profile')}>Profile</button>
          {user?.role === 'ADMIN' && (
            <button className="nav-link" onClick={() => navigate('/admin')}>Admin</button>
          )}
          <div className="nav-divider"></div>
          <button className="btn btn-outline btn-sm" onClick={logout}>Sign Out</button>
        </div>
      </nav>

      <div className="dashboard-container fade-in">
        {/* ── Page Header ── */}
        <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 6, fontWeight: 500 }}>
              Welcome back, {user?.email?.split('@')[0] || 'there'}
            </p>
            <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em' }}>My Resumes</h2>
          </div>
          <div style={{ width: 280 }}>
            <QuotaWidget />
          </div>
        </div>

        {/* ── Stats Row ── */}
        {resumes.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 36 }}>
            <div className="stat-card">
              <div className="stat-label">Total Resumes</div>
              <div className="stat-value">{resumes.length}</div>
              <div className="stat-sub">in your account</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Published</div>
              <div className="stat-value">{resumes.filter(r => r.public).length}</div>
              <div className="stat-sub">in public gallery</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Templates Used</div>
              <div className="stat-value">{new Set(resumes.map(r => r.templateId)).size}</div>
              <div className="stat-sub">unique designs</div>
            </div>
          </div>
        )}

        {/* ── Action Row ── */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <p style={{ fontSize: 14, color: 'var(--text-muted)', margin: 0 }}>
            {resumes.length === 0 ? "You haven't created any resumes yet." : `${resumes.length} resume${resumes.length > 1 ? 's' : ''}`}
          </p>
          <button className="btn btn-primary btn-sm" onClick={handleCreate}>
            <PlusIcon /> New Resume
          </button>
        </div>

        {/* ── Resumes Grid or Empty State ── */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '64px 0' }}>
            <div className="spinner spinner-dark" style={{ margin: '0 auto' }}></div>
          </div>
        ) : resumes.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">
              <FileIcon />
            </div>
            <h3>No resumes yet</h3>
            <p>Create your first resume and start applying with confidence.</p>
            <button className="btn btn-primary" onClick={handleCreate}>
              <PlusIcon /> Create your first resume
            </button>
          </div>
        ) : (
          <div className="resume-grid">
            {resumes.map((r) => (
              <div
                key={r.resumeId}
                className="resume-card"
                onClick={() => navigate(`/builder/${r.resumeId}`)}
              >
                {/* Card header */}
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 6 }}>
                    <h3 style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-0.02em', lineHeight: 1.3 }}>{r.title}</h3>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--text-muted)', padding: '4px 8px', flexShrink: 0 }}
                        onClick={(e) => handleDuplicate(e, r.resumeId)}
                        title="Duplicate resume"
                      >
                        <CopyIcon />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--text-muted)', padding: '4px 8px', flexShrink: 0 }}
                        onClick={(e) => handleDelete(e, r.resumeId)}
                        title="Delete resume"
                      >
                        <TrashIcon />
                      </button>
                    </div>
                  </div>
                  <p style={{ fontSize: 13, margin: 0, color: 'var(--text-muted)' }}>{r.targetJobTitle}</p>
                  {r.public && (
                    <span style={{ fontSize: 10, background: '#dcfce7', color: '#166534', padding: '1px 7px', borderRadius: 999, fontWeight: 700, marginTop: 6, display: 'inline-block' }}>PUBLIC</span>
                  )}
                </div>
              </div>
            ))}

            {/* Add New Card */}
            <div
              className="resume-card"
              onClick={handleCreate}
              style={{
                border: '1.5px dashed var(--border-strong)',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 10,
                color: 'var(--text-muted)',
                background: 'transparent',
                minHeight: 160,
              }}
            >
              <div style={{
                width: 36, height: 36, borderRadius: '50%',
                border: '1.5px solid var(--border-strong)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <PlusIcon />
              </div>
              <span style={{ fontSize: 14, fontWeight: 500 }}>New Resume</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
