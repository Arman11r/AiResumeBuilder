import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

const SearchIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>
);

const BookmarkIcon = ({ filled }) => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill={filled ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
  </svg>
);

const ChevronDown = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="6 9 12 15 18 9"/>
  </svg>
);

const SOURCE_LABELS = {
  linkedin: { label: 'LinkedIn', color: '#0077b5' },
  naukri:   { label: 'Naukri', color: '#ff7555' },
};

export default function JobMatch() {
  const { user } = useContext(AuthContext);
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [resumes, setResumes] = useState([]);
  const [selectedResumeId, setSelectedResumeId] = useState('');
  const [jobTitle, setJobTitle] = useState('');
  const [location, setLocation] = useState('');
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [expandedJob, setExpandedJob] = useState(null);
  const [bookmarking, setBookmarking] = useState(null);

  useEffect(() => {
    const fetchResumes = async () => {
      try {
        const res = await api.get(`/resumes/user/${user.userId}`);
        setResumes(res.data || []);
        if (res.data?.length > 0) setSelectedResumeId(res.data[0].resumeId);
      } catch (_) {}
    };
    fetchResumes();
  }, [user.userId]);

  const handleSearch = async (source) => {
    if (!selectedResumeId || !jobTitle.trim()) {
      return showToast('Select a resume and enter a job title', 'error');
    }
    setLoading(true);
    setJobs([]);
    try {
      const endpoint = source === 'linkedin' ? '/job-matches/fetch-linkedin' : '/job-matches/fetch-naukri';
      const res = await api.post(endpoint, {
        userId: user.userId,
        resumeId: selectedResumeId,
        jobTitle: jobTitle.trim(),
        location: location.trim(),
        limit: 10,
      });
      setJobs(res.data || []);
      const count = res.data?.length || 0;
      showToast(`Found ${count} job${count !== 1 ? 's' : ''} from ${SOURCE_LABELS[source].label}`, 'success');
    } catch (err) {
      showToast('Failed to fetch jobs. Please try again.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleBookmark = async (e, matchId) => {
    e.stopPropagation();
    setBookmarking(matchId);
    try {
      const res = await api.put(`/job-matches/${matchId}/bookmark`);
      setJobs(jobs.map(j => j.matchId === matchId ? { ...j, isBookmarked: res.data.isBookmarked } : j));
    } catch (_) {
      showToast('Could not update bookmark', 'error');
    } finally {
      setBookmarking(null);
    }
  };

  const getScoreColor = (score) => {
    if (score >= 70) return 'var(--success)';
    if (score >= 50) return 'var(--warning)';
    return 'var(--danger)';
  };

  const getScoreBadgeClass = (score) => {
    if (score >= 70) return 'badge-green';
    if (score >= 50) return 'badge-yellow';
    return 'badge-red';
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* Navbar */}
      <nav className="navbar">
        <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          Resume<span>AI</span>
        </div>
        <div className="nav-links">
          <button className="nav-link" onClick={() => navigate('/dashboard')}>Dashboard</button>
          <button className="nav-link active">Job Match</button>
          <button className="nav-link" onClick={() => navigate('/cover-letter')}>Cover Letters</button>
          <div className="nav-divider"></div>
          <span style={{ fontSize: 13, color: 'var(--text-muted)', padding: '0 8px' }}>{user.email}</span>
        </div>
      </nav>

      <div className="dashboard-container fade-in">
        {/* Header */}
        <div style={{ marginBottom: 32 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--primary)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 10 }}>
            Job Match
          </div>
          <h2 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em', marginBottom: 8 }}>
            Find your best-fit roles
          </h2>
          <p style={{ fontSize: 14, maxWidth: 520 }}>
            Search live job listings from LinkedIn and Naukri. Each result is automatically scored against your resume so you know where to apply first.
          </p>
        </div>

        {/* Search Card */}
        <div className="card" style={{ marginBottom: 32 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1.5fr 1.2fr', gap: 16, marginBottom: 20 }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label>Resume</label>
              <select className="input-field" value={selectedResumeId} onChange={e => setSelectedResumeId(e.target.value)}>
                {resumes.length === 0
                  ? <option value="">No resumes found</option>
                  : resumes.map(r => <option key={r.resumeId} value={r.resumeId}>{r.title}</option>)
                }
              </select>
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label>Job Title</label>
              <input
                className="input-field"
                placeholder="e.g. Backend Engineer"
                value={jobTitle}
                onChange={e => setJobTitle(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSearch('linkedin')}
              />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label>Location (optional)</label>
              <input
                className="input-field"
                placeholder="e.g. Remote, Bangalore"
                value={location}
                onChange={e => setLocation(e.target.value)}
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <button
              className="btn btn-primary"
              onClick={() => handleSearch('linkedin')}
              disabled={loading}
            >
              {loading ? <span className="spinner"></span> : <SearchIcon />}
              Search LinkedIn
            </button>
            <button
              className="btn btn-outline"
              onClick={() => handleSearch('naukri')}
              disabled={loading}
            >
              {loading ? <span className="spinner spinner-dark"></span> : <SearchIcon />}
              Search Naukri
            </button>
            {jobs.length > 0 && !loading && (
              <span style={{ marginLeft: 8, fontSize: 13, color: 'var(--text-muted)' }}>
                {jobs.length} result{jobs.length !== 1 ? 's' : ''}
              </span>
            )}
          </div>
        </div>

        {/* Loading State */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '56px 0' }}>
            <div className="spinner spinner-dark" style={{ margin: '0 auto 14px', width: 24, height: 24 }}></div>
            <p style={{ fontSize: 14 }}>Searching live job listings and scoring against your resume...</p>
          </div>
        )}

        {/* Results */}
        {!loading && jobs.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {jobs.map((job) => {
              const isExpanded = expandedJob === job.matchId;
              const scoreColor = getScoreColor(job.matchScore);
              const srcKey = (job.source || 'linkedin').toLowerCase();
              const src = SOURCE_LABELS[srcKey] || SOURCE_LABELS.linkedin;

              return (
                <div
                  key={job.matchId}
                  style={{
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                    overflow: 'hidden',
                    borderLeft: `3px solid ${scoreColor}`,
                    transition: 'box-shadow 0.18s',
                  }}
                >
                  {/* Job Card Header */}
                  <div
                    style={{ padding: '18px 22px', cursor: 'pointer', display: 'flex', gap: 20, alignItems: 'flex-start' }}
                    onClick={() => setExpandedJob(isExpanded ? null : job.matchId)}
                  >
                    {/* Score Circle */}
                    <div style={{
                      width: 52, height: 52, borderRadius: '50%',
                      border: `2.5px solid ${scoreColor}`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      flexShrink: 0,
                      flexDirection: 'column',
                    }}>
                      <span style={{ fontSize: 14, fontWeight: 800, color: scoreColor, lineHeight: 1 }}>{job.matchScore}</span>
                      <span style={{ fontSize: 9, fontWeight: 600, color: scoreColor, letterSpacing: '0.03em' }}>%</span>
                    </div>

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                        <div>
                          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 3, letterSpacing: '-0.02em' }}>{job.jobTitle}</h3>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' }}>
                              {job.companyName || 'Company'}
                            </span>
                            <span style={{
                              fontSize: 11, fontWeight: 700,
                              color: src.color,
                              padding: '2px 7px',
                              border: `1px solid ${src.color}30`,
                              borderRadius: 4,
                              background: `${src.color}10`,
                            }}>
                              {src.label}
                            </span>
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0 }}>
                          <button
                            className="btn btn-ghost btn-sm"
                            style={{ color: job.isBookmarked ? 'var(--primary)' : 'var(--text-muted)', padding: '5px 8px' }}
                            onClick={(e) => handleBookmark(e, job.matchId)}
                            disabled={bookmarking === job.matchId}
                            title={job.isBookmarked ? 'Remove bookmark' : 'Bookmark job'}
                          >
                            <BookmarkIcon filled={job.isBookmarked} />
                          </button>
                          <button
                            className="btn btn-ghost btn-sm"
                            style={{ color: 'var(--text-muted)', padding: '5px 8px', transform: isExpanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}
                          >
                            <ChevronDown />
                          </button>
                        </div>
                      </div>

                      {/* Inline description preview */}
                      {!isExpanded && job.jobDescription && (
                        <p style={{ fontSize: 13, margin: '8px 0 0', color: 'var(--text-muted)', lineHeight: 1.5, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '80%' }}>
                          {job.jobDescription}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Expanded Detail Panel */}
                  {isExpanded && (
                    <div style={{ borderTop: '1px solid var(--border)', padding: '20px 22px', background: 'var(--bg)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
                      {/* Job Description */}
                      <div>
                        <h4 style={{ marginBottom: 10, fontSize: 13, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)' }}>Job Description</h4>
                        <p style={{ fontSize: 13, lineHeight: 1.65, color: 'var(--text-secondary)' }}>
                          {job.jobDescription || 'No description available.'}
                        </p>
                      </div>

                      {/* AI Recommendations */}
                      <div>
                        <h4 style={{ marginBottom: 10, fontSize: 13, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)' }}>How to Improve Your Fit</h4>

                        {/* Missing Skills */}
                        {job.missingSkills && (
                          <div style={{ marginBottom: 14 }}>
                            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--danger)', marginBottom: 6 }}>Missing from your resume</div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                              {job.missingSkills.split(',').filter(Boolean).map((skill, i) => (
                                <span key={i} className="tag tag-red">{skill.trim()}</span>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Recommendations */}
                        {job.recommendations && (
                          <div>
                            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>Recommendations</div>
                            <p style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
                              {job.recommendations}
                            </p>
                          </div>
                        )}

                        <button
                          className="btn btn-outline btn-sm"
                          style={{ marginTop: 16 }}
                          onClick={() => navigate(`/builder/${selectedResumeId}`)}
                        >
                          Improve resume for this role
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Empty — no search yet */}
        {!loading && jobs.length === 0 && (
          <div className="empty-state">
            <div className="empty-icon">
              <SearchIcon />
            </div>
            <h3>Search for jobs</h3>
            <p>Enter a job title and location above, then click Search LinkedIn or Search Naukri to find live listings scored against your resume.</p>
          </div>
        )}
      </div>
    </div>
  );
}
