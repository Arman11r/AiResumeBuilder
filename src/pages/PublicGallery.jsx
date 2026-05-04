import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function PublicGallery() {
  const navigate = useNavigate();
  const [resumes, setResumes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(null);
  const [sections, setSections] = useState([]);
  const [sectionsLoading, setSectionsLoading] = useState(false);

  useEffect(() => {
    api.get('/resumes/public')
      .then(r => setResumes(r.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const openResume = async (r) => {
    setSelected(r);
    setSectionsLoading(true);
    setSections([]);
    try {
      const res = await api.get(`/sections/resume/${r.resumeId}`);
      setSections(res.data || []);
    } catch { setSections([]); }
    finally { setSectionsLoading(false); }
  };

  const filtered = resumes.filter(r =>
    r.title?.toLowerCase().includes(search.toLowerCase()) ||
    r.targetJobTitle?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      <nav className="navbar">
        <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
          Resume<span>AI</span>
        </div>
        <div className="nav-links">
          <button className="nav-link" onClick={() => navigate('/dashboard')}>Dashboard</button>
          <button className="nav-link" onClick={() => navigate('/login')}>Sign In</button>
        </div>
      </nav>

      <div style={{ maxWidth: 1100, margin: '0 auto', padding: '48px 24px' }}>
        <div style={{ textAlign: 'center', marginBottom: 48 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--primary)',
            textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 12 }}>
            Community
          </div>
          <h1 style={{ fontSize: 40, fontWeight: 900, letterSpacing: '-0.04em', marginBottom: 16 }}>
            Public Resume Gallery
          </h1>
          <p style={{ fontSize: 16, color: 'var(--text-muted)', maxWidth: 480, margin: '0 auto 32px' }}>
            Browse resumes shared by the community. Click any card to view the full content.
          </p>
          <input
            className="input-field"
            style={{ maxWidth: 400, margin: '0 auto', display: 'block' }}
            placeholder="Search by title or job role…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 80 }}>
            <div className="spinner spinner-dark" style={{ margin: '0 auto' }} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <h3>{search ? 'No results found' : 'No public resumes yet'}</h3>
            <p>{search ? 'Try a different search term.' : 'Be the first to publish your resume from the Resume Builder!'}</p>
            <button className="btn btn-primary" onClick={() => navigate('/login')}>Get Started</button>
          </div>
        ) : (
          <div className="resume-grid">
            {filtered.map(r => (
              <div key={r.resumeId} className="resume-card fade-in"
                style={{ cursor: 'pointer', transition: 'transform 0.15s, box-shadow 0.15s' }}
                onClick={() => openResume(r)}
                onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.boxShadow = 'var(--shadow-lg)'; }}
                onMouseLeave={e => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = ''; }}>
                <div style={{ padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                    <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700 }}>{r.title}</h3>
                    <span style={{ fontSize: 10, background: '#dcfce7', color: '#166534',
                      padding: '2px 8px', borderRadius: 999, fontWeight: 700, flexShrink: 0 }}>PUBLIC</span>
                  </div>
                  {r.targetJobTitle && (
                    <div style={{ fontSize: 12.5, color: 'var(--primary)', fontWeight: 600, marginBottom: 8 }}>
                      🎯 {r.targetJobTitle}
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: 16, fontSize: 12, color: 'var(--text-muted)', marginBottom: 8 }}>
                    {r.atsScore != null && (
                      <span>📊 ATS: <strong style={{ color: 'var(--success)' }}>{r.atsScore}%</strong></span>
                    )}
                    <span>👁 {r.viewCount || 0} views</span>
                    <span style={{ textTransform: 'capitalize' }}>📋 {(r.status || 'draft').toLowerCase()}</span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                    Updated {r.updatedAt ? new Date(r.updatedAt).toLocaleDateString('en-US',
                      { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
                  </div>
                  <div style={{ marginTop: 12, padding: '6px 12px', background: 'var(--primary-light)',
                    borderRadius: 6, fontSize: 12, color: 'var(--primary)', fontWeight: 600, textAlign: 'center' }}>
                    Click to view →
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Resume Viewer Modal */}
      {selected && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: 24, backdropFilter: 'blur(4px)' }}
          onClick={e => { if (e.target === e.currentTarget) setSelected(null); }}>
          <div style={{ background: 'var(--surface)', borderRadius: 16, width: '100%',
            maxWidth: 720, maxHeight: '90vh', display: 'flex', flexDirection: 'column',
            boxShadow: '0 24px 64px rgba(0,0,0,0.3)' }}>

            {/* Modal header */}
            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border)',
              display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800 }}>{selected.title}</h2>
                {selected.targetJobTitle && (
                  <div style={{ fontSize: 13, color: 'var(--primary)', fontWeight: 600, marginTop: 4 }}>
                    🎯 {selected.targetJobTitle}
                  </div>
                )}
                <div style={{ display: 'flex', gap: 16, marginTop: 8, fontSize: 12, color: 'var(--text-muted)' }}>
                  {selected.atsScore != null && <span>📊 ATS Score: <strong>{selected.atsScore}%</strong></span>}
                  <span>👁 {selected.viewCount || 0} views</span>
                  <span>Language: {selected.language?.toUpperCase()}</span>
                </div>
              </div>
              <button onClick={() => setSelected(null)}
                style={{ background: 'var(--bg)', border: '1px solid var(--border)',
                  borderRadius: 8, padding: '6px 12px', cursor: 'pointer',
                  fontSize: 13, color: 'var(--text-muted)', flexShrink: 0 }}>
                ✕ Close
              </button>
            </div>

            {/* Sections */}
            <div style={{ overflowY: 'auto', padding: '24px', flex: 1 }}>
              {sectionsLoading ? (
                <div style={{ textAlign: 'center', padding: 40 }}>
                  <div className="spinner spinner-dark" style={{ margin: '0 auto' }} />
                  <p style={{ marginTop: 12, color: 'var(--text-muted)' }}>Loading resume content…</p>
                </div>
              ) : sections.length === 0 ? (
                <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
                  <div style={{ fontSize: 32, marginBottom: 8 }}>📄</div>
                  <p>No content sections found for this resume.</p>
                </div>
              ) : (
                sections
                  .filter(s => s.visible !== false)
                  .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
                  .map(s => (
                    <div key={s.sectionId} style={{ marginBottom: 24 }}>
                      <div style={{ fontSize: 11, fontWeight: 800, color: 'var(--primary)',
                        textTransform: 'uppercase', letterSpacing: '0.08em',
                        marginBottom: 8, paddingBottom: 6,
                        borderBottom: '2px solid var(--primary-light)' }}>
                        {s.title || s.sectionType}
                      </div>
                      <div style={{ fontSize: 14, lineHeight: 1.7, color: 'var(--text-secondary)',
                        whiteSpace: 'pre-wrap' }}>
                        {s.content || <em style={{ color: 'var(--text-muted)' }}>No content</em>}
                      </div>
                    </div>
                  ))
              )}
            </div>

            {/* Footer */}
            <div style={{ padding: '16px 24px', borderTop: '1px solid var(--border)',
              background: 'var(--bg)', borderRadius: '0 0 16px 16px',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                Want to create a resume like this?
              </span>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
                Build Your Resume →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
