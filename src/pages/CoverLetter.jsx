import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { pushNotification } from '../services/notify';
import { useNavigate, useParams } from 'react-router-dom';
import NotificationDropdown from '../components/NotificationDropdown';

const MailIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
    <polyline points="22,6 12,13 2,6"/>
  </svg>
);

const SparkleIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z"/>
  </svg>
);

export default function CoverLetter() {
  const { resumeId: paramResumeId } = useParams();
  const { user } = useContext(AuthContext);
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [resumes, setResumes] = useState([]);
  const [selectedResumeId, setSelectedResumeId] = useState(paramResumeId || '');
  const [jobTitle, setJobTitle] = useState('');
  const [jobDesc, setJobDesc] = useState('');
  const [letter, setLetter] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchResumes = async () => {
      try {
        const res = await api.get(`/resumes/user/${user.userId}`);
        setResumes(res.data || []);
        if (res.data?.length > 0 && !selectedResumeId) setSelectedResumeId(res.data[0].resumeId);
      } catch (_) {}
    };
    fetchResumes();
  }, [user.userId]);

  const handleGenerate = async () => {
    if (!selectedResumeId || !jobDesc.trim()) {
      return showToast('Select a resume and enter job description', 'error');
    }
    setLoading(true);
    try {
      const res = await api.post('/ai/generateCoverLetter', {
        userId: user.userId,
        resumeId: selectedResumeId,
        jobTitle: jobTitle.trim() || 'Professional',
        jobDescription: jobDesc.trim(),
      });
      setLetter(res.data?.content || 'Failed to generate content');
      showToast('Cover letter generated ✨', 'success');
      pushNotification({
        recipientId: user.userId,
        type: 'COVER_LETTER_GENERATED',
        message: 'Your AI-generated cover letter is ready! Review it in the preview panel and copy or save it.',
        relatedId: selectedResumeId,
      });
    } catch (err) {
      showToast(err.response?.data?.message || err.response?.data?.error || 'Failed to generate cover letter. Try again.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      <nav className="navbar">
        <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          Resume<span>AI</span>
        </div>
        <div className="nav-links">
          <button className="nav-link" onClick={() => navigate('/dashboard')}>Dashboard</button>
          <button className="nav-link" onClick={() => navigate('/job-match')}>Job Match</button>
          <button className="nav-link active">Cover Letters</button>
          <div className="nav-divider"></div>
          <NotificationDropdown userId={user.userId} />
          <span style={{ fontSize: 13, color: 'var(--text-muted)', padding: '0 8px' }}>{user.email}</span>
        </div>
      </nav>

      <div className="dashboard-container fade-in" style={{ display: 'grid', gridTemplateColumns: '1fr 1.3fr', gap: 48 }}>
        
        {/* ── Editor Pane ── */}
        <div>
          <div style={{ marginBottom: 32 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--primary)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 10 }}>
              Cover Letter
            </div>
            <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em', marginBottom: 8 }}>
              Generate tailored letters
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-muted)' }}>
              Create a highly personalized cover letter based on your resume and the exact target job description.
            </p>
          </div>
          
          <div className="card">
            <div className="form-group">
              <label>Select Source Resume</label>
              <select className="input-field" value={selectedResumeId} onChange={e => setSelectedResumeId(e.target.value)}>
                {resumes.length === 0 ? <option value="">No resumes found</option> : resumes.map(r => <option key={r.resumeId} value={r.resumeId}>{r.title}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Target Job Title</label>
              <input
                className="input-field"
                value={jobTitle}
                onChange={e => setJobTitle(e.target.value)}
                placeholder="e.g. Senior Software Engineer"
              />
            </div>
            <div className="form-group" style={{ marginBottom: 24 }}>
              <label>Target Job Description</label>
              <textarea 
                className="input-field" 
                rows={12} 
                value={jobDesc} 
                onChange={e => setJobDesc(e.target.value)} 
                placeholder="Paste the exact job description you are applying for..."
              />
            </div>
            <button className="btn btn-primary" style={{ width: '100%', fontSize: 15, padding: '12px' }} onClick={handleGenerate} disabled={loading}>
              {loading ? <span className="spinner"></span> : <><SparkleIcon /> Generate Cover Letter</>}
            </button>
          </div>
        </div>

        {/* ── Preview Pane ── */}
        <div>
          <div className="resume-paper" style={{ minHeight: 800, padding: 64, boxShadow: 'var(--shadow-sm)' }}>
            {letter ? (
              <div style={{ whiteSpace: 'pre-wrap', fontFamily: 'Georgia, serif', fontSize: 15, lineHeight: 1.7, color: '#1f2937' }} className="fade-in">
                {letter}
              </div>
            ) : (
              <div style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                <MailIcon />
                <p style={{ marginTop: 16, fontSize: 14 }}>Your AI-generated cover letter will appear here.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
