import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useToast } from '../context/ToastContext';
import { AuthContext } from '../context/AuthContext';
import ResumePreview from '../components/ResumePreview';
import html2pdf from 'html2pdf.js';
import { saveAs } from 'file-saver';
import QuotaWidget from '../components/QuotaWidget';

// ── SVG Icons ────────────────────────────────────────────────────────────────
const SparkleIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z"/>
  </svg>
);
const TrashIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/>
    <path d="M10 11v6"/><path d="M14 11v6"/>
  </svg>
);
const WandIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M15 4V2"/><path d="M15 16v-2"/><path d="M8 9h2"/><path d="M20 9h2"/>
    <path d="M17.8 11.8L19 13"/><path d="M15 9h.01"/><path d="M17.8 6.2L19 5"/>
    <path d="M3 21l9-9"/><path d="M12.2 6.2L11 5"/>
  </svg>
);
const DownloadIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
    <polyline points="7 10 12 15 17 10"/>
    <line x1="12" y1="15" x2="12" y2="3"/>
  </svg>
);
const ContentIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <line x1="17" y1="10" x2="3" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/>
    <line x1="21" y1="14" x2="3" y2="14"/><line x1="17" y1="18" x2="3" y2="18"/>
  </svg>
);
const AtsIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
  </svg>
);

// ── AI Tips per section type ──────────────────────────────────────────────────
const AI_TIPS = {
  SUMMARY: [
    'Start with a strong professional identity statement',
    'Include years of experience and top 2–3 skills',
    'End with a clear value proposition for the employer',
  ],
  EXPERIENCE: [
    'Begin each bullet with a past-tense action verb',
    'Quantify achievements: percentages, dollar amounts, team sizes',
    'Focus on impact, not just responsibilities',
  ],
  SKILLS: [
    'Mirror exact skill names from the job description',
    'Group by category: Languages, Frameworks, Tools, Soft Skills',
    'Include both technical and transferable skills',
  ],
  EDUCATION: [
    'Include GPA if above 3.5 / 8.0 CGPA',
    'List relevant coursework for entry-level roles',
    'Add certifications and online courses',
  ],
  PROJECTS: [
    'Include a one-line description of the project goal',
    'Mention the tech stack used',
    'Link to live demo or GitHub where possible',
  ],
};

export default function ResumeBuilder() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { user } = useContext(AuthContext);

  const [resume, setResume] = useState(null);
  const [sections, setSections] = useState([]);
  const [activeTab, setActiveTab] = useState('content');
  const [focusedSection, setFocusedSection] = useState(null);

  // ATS State
  const [jobDesc, setJobDesc] = useState('');
  const [atsResult, setAtsResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);

  useEffect(() => { loadData(); }, [id]);

  const loadData = async () => {
    try {
      const [rRes, sRes] = await Promise.all([
        api.get(`/resumes/${id}`),
        api.get(`/sections/resume/${id}`),
      ]);
      setResume(rRes.data);
      setSections(sRes.data || []);
    } catch (err) {
      showToast('Failed to load resume data', 'error');
    }
  };

  const handleUpdateResume = async (field, value) => {
    const updated = { ...resume, [field]: value };
    setResume(updated);
    try {
      await api.put(`/resumes/${id}`, updated);
    } catch (_) { showToast('Failed to save', 'error'); }
  };

  const handleAddSection = async (type) => {
    try {
      const newSec = { resumeId: id, sectionType: type, title: type.charAt(0) + type.slice(1).toLowerCase(), content: '', displayOrder: sections.length, visible: true };
      await api.post('/sections', newSec);
      loadData();
      showToast(`${type} section added`, 'success');
    } catch (_) { showToast('Error adding section', 'error'); }
  };

  const handleUpdateSection = async (sectionId, field, value) => {
    const sec = sections.find(s => s.sectionId === sectionId);
    const updated = { ...sec, [field]: value };
    setSections(sections.map(s => s.sectionId === sectionId ? updated : s));
    try {
      await api.put(`/sections/${sectionId}`, updated);
    } catch (_) { showToast('Failed to save section', 'error'); }
  };

  const handleDeleteSection = async (sectionId) => {
    try {
      await api.delete(`/sections/${sectionId}`);
      loadData();
      showToast('Section deleted', 'success');
    } catch (_) { showToast('Error deleting', 'error'); }
  };

  const callAi = async (endpoint, payload, sectionId) => {
    setAiLoading(true);
    try {
      const res = await api.post(endpoint, {
        userId: user.userId,
        resumeId: id,
        jobTitle: resume.targetJobTitle || 'Professional',
        ...payload,
      });
      if (sectionId && res.data.content) {
        await handleUpdateSection(sectionId, 'content', res.data.content);
        showToast('AI content generated', 'success');
      }
      return res.data;
    } catch (err) {
      showToast(err.response?.data?.message || err.response?.data?.error || 'AI request failed', 'error');
    } finally {
      setAiLoading(false);
    }
  };

  const handleAtsCheck = async () => {
    if (!jobDesc) return showToast('Please enter a job description', 'error');
    const content = sections.map(s => s.content).join('\n');
    const res = await callAi('/ai/checkAts', { sectionContent: content, jobDescription: jobDesc });
    if (res) {
      setAtsResult(res);
      api.post('/notifications', {
        recipientId: user.userId,
        title: 'ATS Check Complete',
        type: 'SYSTEM_ALERT',
        message: `ATS check complete! Your score is ${res.score}%`,
        relatedId: id,
      }).catch(() => {});
    }
  };

  const handleExport = async (format) => {
    setAiLoading(true);
    try {
      const element = document.querySelector('.resume-paper');
      if (!element) return showToast('Preview not found', 'error');

      if (format === 'PDF') {
        showToast('Generating PDF...', 'info');
        await html2pdf().set({
          margin: [0.4, 0.4, 0.4, 0.4],
          filename: `${resume.title || 'Resume'}.pdf`,
          image: { type: 'jpeg', quality: 0.99 },
          html2canvas: {
            scale: 2,
            useCORS: true,
            logging: false,
            backgroundColor: '#ffffff',
            onclone: (clonedDoc) => {
              // Resolve CSS variables to real colors so PDF looks identical to screen
              const clonedEl = clonedDoc.querySelector('.resume-paper');
              if (clonedEl) {
                clonedEl.style.background = '#ffffff';
                clonedEl.style.color = '#1f2937';
                const names = clonedDoc.querySelectorAll('.preview-name');
                names.forEach(el => { el.style.color = '#2563eb'; });
                const titles = clonedDoc.querySelectorAll('.preview-section-title');
                titles.forEach(el => {
                  el.style.color = '#2563eb';
                  el.style.borderBottomColor = '#2563eb';
                });
                const muted = clonedDoc.querySelectorAll('.preview-title');
                muted.forEach(el => { el.style.color = '#6b7280'; });
              }
            },
          },
          jsPDF: { unit: 'in', format: 'a4', orientation: 'portrait' },
        }).from(element).save();
        showToast('PDF downloaded', 'success');

      } else if (format === 'DOCX') {
        showToast('Generating DOCX...', 'info');
        // Escape HTML entities in user content to avoid malformed XML
        const esc = (str) => (str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');

        const sectionHTML = sections
          .filter(s => s.visible && s.content)
          .sort((a, b) => a.displayOrder - b.displayOrder)
          .map(s => `<div style="margin-top:18px">
            <div style="font-size:13pt;font-weight:bold;color:#2563eb;text-transform:uppercase;border-bottom:2px solid #2563eb;padding-bottom:3px;margin-bottom:8px">${esc(s.title)}</div>
            <div style="font-size:11pt;color:#1f2937;line-height:1.6;white-space:pre-wrap">${esc(s.content)}</div>
          </div>`)
          .join('');

        const wordHtml = `<!DOCTYPE html><html><head><meta charset="UTF-8">
          <style>
            body { font-family: Arial, sans-serif; margin: 60px; color: #1f2937; }
            h1   { font-size: 22pt; color: #2563eb; text-align: center; margin-bottom: 2px; }
            .sub { font-size: 13pt; color: #6b7280; text-align: center; margin-bottom: 18px; }
            hr   { border: 1.5px solid #2563eb; margin-bottom: 18px; }
          </style></head><body>
          <h1>${esc(resume.title)}</h1>
          <div class="sub">${esc(resume.targetJobTitle)}</div>
          <hr>${sectionHTML}</body></html>`;

        // Use application/msword MIME — universally recognised by Word / LibreOffice
        const blob = new Blob(['\ufeff', wordHtml], { type: 'application/msword;charset=utf-8' });
        saveAs(blob, `${resume.title || 'Resume'}.doc`);
        showToast('Document downloaded (.doc)', 'success');
      }

      api.post('/notifications', {
        recipientId: user.userId,
        title: 'Export Ready',
        type: 'EXPORT_READY',
        message: `Your ${format} export is ready!`,
        relatedId: id,
      }).catch(() => {});
    } catch (err) {
      showToast('Export failed', 'error');
    } finally {
      setAiLoading(false);
    }
  };


  if (!resume) return (
    <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="spinner spinner-dark"></div>
    </div>
  );

  const focusedSec = sections.find(s => s.sectionId === focusedSection);
  const tips = focusedSec ? (AI_TIPS[focusedSec.sectionType] || AI_TIPS.EXPERIENCE) : null;

  return (
    <div className="builder-layout fade-in">
      {/* ── Sidebar ── */}
      <div className="builder-sidebar">
        <div className="sidebar-header">
          <div
            style={{ fontWeight: 800, color: 'var(--text-main)', fontSize: 15, cursor: 'pointer', letterSpacing: '-0.03em' }}
            onClick={() => navigate('/dashboard')}
          >
            <span style={{ color: 'var(--primary)' }}>Resume</span>AI
          </div>
        </div>

        <div className="sidebar-nav">
          <button className={`nav-item ${activeTab === 'content' ? 'active' : ''}`} onClick={() => setActiveTab('content')}>
            <ContentIcon /> Content
          </button>
          <button className={`nav-item ${activeTab === 'ats' ? 'active' : ''}`} onClick={() => setActiveTab('ats')}>
            <AtsIcon /> ATS Match
          </button>
        </div>


        {/* Quota Widget */}
        <div style={{ padding: '0 16px', marginTop: 16 }}>
          <QuotaWidget />
        </div>

        {/* Export */}
        <div style={{ padding: '16px', borderTop: '1px solid var(--border)', marginTop: 'auto' }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
            Export
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <button className="btn btn-outline btn-sm" onClick={() => handleExport('PDF')} disabled={aiLoading}>
              <DownloadIcon /> Download PDF
            </button>
          </div>
        </div>
      </div>

      {/* ── Editor Pane ── */}
      <div className="builder-editor">
        {activeTab === 'content' && (
          <div className="fade-in">
            <div className="editor-header">
              <h2 style={{ fontSize: 22 }}>Content Editor</h2>
              <p style={{ fontSize: 14 }}>Fill in your details or use AI to generate professional content.</p>
            </div>

            {/* Personal Info */}
            <div className="section-card">
              <h3 style={{ marginBottom: 16, fontSize: 15 }}>Personal Information</h3>
              <div className="form-group">
                <label>Full Name / Resume Title</label>
                <input className="input-field" value={resume.title || ''} onChange={e => handleUpdateResume('title', e.target.value)} placeholder="e.g. John Doe" />
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label>Target Job Title</label>
                <input className="input-field" value={resume.targetJobTitle || ''} onChange={e => handleUpdateResume('targetJobTitle', e.target.value)} placeholder="e.g. Senior Software Engineer" />
              </div>
            </div>

            {/* Sections */}
            {sections.map((sec) => (
              <div key={sec.sectionId} className="section-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                  <input
                    className="input-field"
                    style={{ width: '55%', fontWeight: 700, fontSize: 14 }}
                    value={sec.title || ''}
                    onChange={e => handleUpdateSection(sec.sectionId, 'title', e.target.value)}
                  />
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ color: 'var(--danger)', gap: 5 }}
                    onClick={() => handleDeleteSection(sec.sectionId)}
                  >
                    <TrashIcon /> Delete
                  </button>
                </div>

                <textarea
                  className="input-field"
                  rows={6}
                  value={sec.content || ''}
                  onChange={e => handleUpdateSection(sec.sectionId, 'content', e.target.value)}
                  onFocus={() => setFocusedSection(sec.sectionId)}
                  placeholder={`Write your ${sec.sectionType.toLowerCase()} here...`}
                />

                {/* AI Action Buttons */}
                <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
                  {sec.sectionType === 'SUMMARY' && (
                    <button className="btn btn-ai btn-sm" disabled={aiLoading} onClick={() => callAi('/ai/generateSummary', { yearsOfExperience: 3 }, sec.sectionId)}>
                      <SparkleIcon /> Generate Summary
                    </button>
                  )}
                  {sec.sectionType === 'EXPERIENCE' && (
                    <button className="btn btn-ai btn-sm" disabled={aiLoading} onClick={() => callAi('/ai/generateBullets', { sectionContent: sec.content || 'Developed application' }, sec.sectionId)}>
                      <SparkleIcon /> Generate Bullets
                    </button>
                  )}
                  {sec.sectionType === 'SKILLS' && (
                    <button className="btn btn-ai btn-sm" disabled={aiLoading} onClick={() => callAi('/ai/suggestSkills', {}, sec.sectionId)}>
                      <SparkleIcon /> Suggest Skills
                    </button>
                  )}
                  <button className="btn btn-outline btn-sm" disabled={aiLoading} onClick={() => callAi('/ai/improveSection', { sectionContent: sec.content, improveGoal: 'Make it professional and impactful' }, sec.sectionId)}>
                    <WandIcon /> Improve Text
                  </button>
                </div>
              </div>
            ))}

            {/* Add Sections */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
              {['SUMMARY', 'EXPERIENCE', 'EDUCATION', 'SKILLS', 'PROJECTS'].map(type => (
                <button key={type} className="btn btn-outline btn-sm" onClick={() => handleAddSection(type)}>
                  + {type.charAt(0) + type.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>
        )}

        {activeTab === 'ats' && (
          <div className="fade-in">
            <div className="editor-header">
              <h2 style={{ fontSize: 22 }}>ATS Match Analysis</h2>
              <p style={{ fontSize: 14 }}>Compare your resume against a job description and get a real-time compatibility score.</p>
            </div>

            <div className="section-card">
              <div className="form-group">
                <label>Target Job Description</label>
                <textarea
                  className="input-field"
                  rows={9}
                  value={jobDesc}
                  onChange={e => setJobDesc(e.target.value)}
                  placeholder="Paste the full job description here..."
                />
              </div>
              <button className="btn btn-primary" style={{ width: '100%' }} disabled={aiLoading} onClick={handleAtsCheck}>
                {aiLoading ? <><span className="spinner"></span> Analysing...</> : <><AtsIcon /> Run ATS Check</>}
              </button>
            </div>

            {atsResult && (
              <div className="section-card fade-in" style={{
                borderLeft: `4px solid ${atsResult.score >= 70 ? 'var(--success)' : atsResult.score >= 50 ? 'var(--warning)' : 'var(--danger)'}`,
              }}>
                {/* Score */}
                <div style={{ textAlign: 'center', marginBottom: 24 }}>
                  <div style={{
                    fontSize: 56,
                    fontWeight: 900,
                    letterSpacing: '-0.06em',
                    lineHeight: 1,
                    color: atsResult.score >= 70 ? 'var(--success)' : atsResult.score >= 50 ? 'var(--warning)' : 'var(--danger)',
                  }}>
                    {atsResult.score}%
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-muted)', marginTop: 4 }}>Match Score</div>
                  {/* Progress bar */}
                  <div style={{ height: 6, background: 'var(--bg)', borderRadius: 99, margin: '14px auto 0', maxWidth: 280 }}>
                    <div style={{
                      height: '100%',
                      width: `${atsResult.score}%`,
                      borderRadius: 99,
                      background: atsResult.score >= 70 ? 'var(--success)' : atsResult.score >= 50 ? 'var(--warning)' : 'var(--danger)',
                      transition: 'width 0.6s ease',
                    }}></div>
                  </div>
                </div>

                {/* Present Keywords */}
                {atsResult.presentKeywords && (
                  <div style={{ marginBottom: 16 }}>
                    <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--success)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                      Keywords Found
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                      {atsResult.presentKeywords.split(',').filter(Boolean).map((k, i) => (
                        <span key={i} className="tag tag-green">{k.trim()}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Missing Keywords */}
                {atsResult.missingKeywords && (
                  <div style={{ marginBottom: 16 }}>
                    <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--danger)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                      Missing Keywords
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                      {atsResult.missingKeywords.split(',').filter(Boolean).map((k, i) => (
                        <span key={i} className="tag tag-red">{k.trim()}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Suggestions */}
                {atsResult.suggestions && (
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                      Suggestions
                    </div>
                    <ul style={{ paddingLeft: 18, margin: 0, display: 'flex', flexDirection: 'column', gap: 6 }}>
                      {atsResult.suggestions.split('|').map((s, i) => (
                        <li key={i} style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>{s.trim()}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── Preview Pane ── */}
      <div className="builder-preview">
        <ResumePreview resume={resume} sections={sections} />
      </div>

      {/* ── AI Loading Overlay ── */}
      {aiLoading && (
        <div className="modal-overlay">
          <div style={{
            background: 'var(--surface)',
            padding: '20px 36px',
            borderRadius: 12,
            fontWeight: 600,
            color: 'var(--primary)',
            boxShadow: 'var(--shadow-lg)',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            border: '1px solid var(--border)',
          }}>
            <div className="spinner spinner-dark"></div>
            AI is generating content...
          </div>
        </div>
      )}
    </div>
  );
}
