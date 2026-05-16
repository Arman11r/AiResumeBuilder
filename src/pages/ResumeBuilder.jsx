import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useToast } from '../context/ToastContext';
import { AuthContext } from '../context/AuthContext';
import { pushNotification } from '../services/notify';
import ResumePreview from '../components/ResumePreview';
import html2pdf from 'html2pdf.js';
import { saveAs } from 'file-saver';
import {
  Document, Packer, Paragraph, TextRun, HeadingLevel,
  AlignmentType, BorderStyle,
} from 'docx';
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
      pushNotification({
        recipientId: user.userId,
        type: 'SECTION_ADDED',
        message: `A new “${type.charAt(0) + type.slice(1).toLowerCase()}” section was added to your resume.`,
        relatedId: id,
      });
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
      const sec = sections.find(s => s.sectionId === sectionId);
      await api.delete(`/sections/${sectionId}`);
      loadData();
      showToast('Section deleted', 'success');
      pushNotification({
        recipientId: user.userId,
        type: 'SECTION_DELETED',
        message: `Section “${sec?.title || 'Unknown'}” was removed from your resume.`,
        relatedId: id,
      });
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
        showToast('AI content generated ✨', 'success');
        pushNotification({
          recipientId: user.userId,
          type: 'AI_CONTENT_GENERATED',
          message: `AI successfully generated content for a resume section. Review and refine it to match your experience.`,
          relatedId: id,
        });
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
      const score = res.score ?? 0;
      const level = score >= 70 ? 'excellent' : score >= 50 ? 'moderate' : 'low';
      pushNotification({
        recipientId: user.userId,
        type: 'ATS_COMPLETE',
        message: `ATS check complete! Your match score is ${score}% (${level}). ${score < 70 ? 'Check the missing keywords tab to improve.' : 'Great job — your resume is highly optimised!'}`,
        relatedId: id,
      });
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

        // Build docx paragraphs from resume sections
        const children = [];

        // ── Name / Title header ──
        children.push(
          new Paragraph({
            text: resume.title || 'Your Name',
            alignment: AlignmentType.LEFT,
            spacing: { after: 60 },
            run: { color: '0D1117', bold: true, size: 52, font: 'Arial' },
          }),
        );
        if (resume.targetJobTitle) {
          children.push(
            new Paragraph({
              alignment: AlignmentType.LEFT,
              spacing: { after: 240 },
              children: [
                new TextRun({
                  text: resume.targetJobTitle,
                  color: '555F70',
                  size: 26,
                  font: 'Arial',
                }),
              ],
            }),
          );
        }

        // ── Sections ──
        const visibleSections = sections
          .filter(s => s.visible !== false && s.content)
          .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

        visibleSections.forEach(s => {
          // Section heading
          children.push(
            new Paragraph({
              spacing: { before: 240, after: 80 },
              border: {
                bottom: { color: 'E4E7EC', style: BorderStyle.SINGLE, size: 6, space: 4 },
              },
              children: [
                new TextRun({
                  text: (s.title || s.sectionType || '').toUpperCase(),
                  bold: true,
                  color: '2563EB',
                  size: 20,
                  font: 'Arial',
                }),
              ],
            }),
          );

          // Section content — split on newlines to preserve paragraph breaks
          const lines = (s.content || '').split('\n');
          lines.forEach(line => {
            children.push(
              new Paragraph({
                spacing: { after: 60 },
                children: [
                  new TextRun({
                    text: line,
                    size: 22,
                    color: '2D3748',
                    font: 'Arial',
                  }),
                ],
              }),
            );
          });
        });

        const doc = new Document({
          creator: 'ResumeAI',
          title: resume.title || 'Resume',
          description: resume.targetJobTitle || '',
          sections: [{
            properties: {
              page: {
                margin: { top: 720, right: 720, bottom: 720, left: 720 },
              },
            },
            children,
          }],
        });

        const blob = await Packer.toBlob(doc);
        saveAs(blob, `${resume.title || 'Resume'}.docx`);
        showToast('DOCX downloaded ✓', 'success');
      }

      pushNotification({
        recipientId: user.userId,
        type: 'EXPORT_READY',
        message: `Your resume “${resume.title || 'Resume'}” has been exported as ${format}. Check your downloads folder.`,
        relatedId: id,
      });
    } catch (err) {
      showToast('Export failed', 'error');
    } finally {
      setAiLoading(false);
    }
  };

  const handlePublishToggle = async () => {
    const willBePublic = !resume.public;
    const endpoint = resume.public ? `/resumes/${id}/unpublish` : `/resumes/${id}/publish`;
    // Optimistic update so button flips immediately
    setResume(prev => ({ ...prev, public: willBePublic }));
    try {
      const res = await api.put(endpoint);
      // Sync with server response (field is 'public' not 'isPublic' in JSON)
      const nowPublic = res.data.public;
      setResume(res.data);
      showToast(nowPublic ? 'Resume published to gallery 🌐' : 'Resume unpublished', nowPublic ? 'success' : 'info');
      pushNotification({
        recipientId: user.userId,
        type: nowPublic ? 'RESUME_PUBLISHED' : 'RESUME_UNPUBLISHED',
        message: nowPublic
          ? `Your resume "${resume.title}" is now live in the public gallery. Others can discover and view it!`
          : `Your resume "${resume.title}" has been removed from the public gallery.`,
        relatedId: id,
      });
    } catch (_) {
      // Revert optimistic update on error
      setResume(prev => ({ ...prev, public: !willBePublic }));
      showToast('Failed to update publish status', 'error');
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

        {/* Publish Toggle */}
        <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
            Visibility
          </div>
          <button
            onClick={handlePublishToggle}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: 8,
              border: `1.5px solid ${resume.public ? 'var(--success)' : 'var(--border-strong)'}`,
              background: resume.public ? 'var(--success-light)' : 'var(--surface)',
              color: resume.public ? 'var(--success)' : 'var(--text-muted)',
              fontFamily: 'inherit',
              fontSize: 12.5,
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 7,
              transition: 'all 0.2s',
            }}
          >
            <span style={{ fontSize: 14 }}>{resume.public ? '🌐' : '🔒'}</span>
            {resume.public ? 'Published — click to unpublish' : 'Private — click to publish'}
          </button>
          {resume.public && (
            <div style={{ fontSize: 11, color: 'var(--success)', marginTop: 5, textAlign: 'center' }}>
              Live in Public Gallery
            </div>
          )}
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
            <button className="btn btn-outline btn-sm" onClick={() => handleExport('DOCX')} disabled={aiLoading}>
              <DownloadIcon /> Download DOCX
            </button>
          </div>
          {resume.public && (
            <button
              className="btn btn-ghost btn-sm"
              style={{ marginTop: 10, width: '100%', fontSize: 11, color: 'var(--primary)' }}
              onClick={() => {
                const url = `${window.location.origin}/gallery`;
                navigator.clipboard.writeText(url).then(() => showToast('Gallery link copied! 🔗', 'success'));
              }}
            >
              🔗 Copy Share Link
            </button>
          )}
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

            {/* ── Premium AI Tools ── */}
            <div className="section-card" style={{ borderLeft: '3px solid var(--primary)' }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--primary)', marginBottom: 12, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                ✨ Premium AI Tools
              </div>

              {/* Tailor Resume */}
              <div style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Tailor Resume for this Job</div>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 10 }}>AI fully rewrites your resume content to match the job description above.</p>
                <button
                  className="btn btn-ai btn-sm"
                  disabled={aiLoading || !jobDesc}
                  onClick={() => {
                    const content = sections.map(s => s.content).join('\n');
                    callAi('/ai/tailorResume', { sectionContent: content, jobDescription: jobDesc }, sections[0]?.sectionId);
                  }}
                >
                  <SparkleIcon /> Tailor Resume
                </button>
              </div>

              {/* Translate Resume */}
              <div>
                <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Translate Resume</div>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 10 }}>Translate your resume content to another language while maintaining professional tone.</p>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <select
                    id="translateLang"
                    className="input-field"
                    style={{ flex: 1 }}
                    defaultValue="French"
                  >
                    {['French','German','Spanish','Portuguese','Hindi','Arabic','Japanese','Chinese'].map(lang => (
                      <option key={lang} value={lang}>{lang}</option>
                    ))}
                  </select>
                  <button
                    className="btn btn-outline btn-sm"
                    disabled={aiLoading}
                    onClick={() => {
                      const content = sections.map(s => s.content).join('\n');
                      const lang = document.getElementById('translateLang')?.value || 'French';
                      callAi('/ai/translate', { sectionContent: content, targetLanguage: lang }, sections[0]?.sectionId);
                    }}
                  >
                    Translate
                  </button>
                </div>
              </div>
            </div>
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
