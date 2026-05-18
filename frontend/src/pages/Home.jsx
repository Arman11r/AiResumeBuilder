import React, { useState, useEffect, useContext } from 'react';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const CATEGORY_COLORS = {
  'Professional': '#3b5bdb',
  'Creative': '#7048e8',
  'Minimal': '#0d9c6e',
  'Executive': '#c92a2a',
  'Modern': '#1971c2',
  'Technical': '#2b8a3e',
};

const MOCK_TEMPLATES = [
  { templateId: 'tmpl-001', name: 'Executive Classic', category: 'Professional', isPremium: false },
  { templateId: 'tmpl-002', name: 'Modern Minimal', category: 'Minimal', isPremium: false },
  { templateId: 'tmpl-003', name: 'Creative Edge', category: 'Creative', isPremium: true },
  { templateId: 'tmpl-004', name: 'Tech Stack', category: 'Technical', isPremium: false },
  { templateId: 'tmpl-005', name: 'Corporate Leader', category: 'Executive', isPremium: true },
  { templateId: 'tmpl-006', name: 'Clean Slate', category: 'Modern', isPremium: false },
];

const STATS = [
  { value: '14,200+', label: 'Resumes Built' },
  { value: '82%', label: 'Average ATS Score' },
  { value: '6', label: 'Professional Templates' },
  { value: '3 min', label: 'Average Build Time' },
];

const STEPS = [
  {
    num: '01',
    title: 'Choose a Template',
    desc: 'Pick from professionally designed layouts optimised for modern ATS systems.',
  },
  {
    num: '02',
    title: 'Add Your Content',
    desc: 'Fill in your details or let AI generate and improve your content in seconds.',
  },
  {
    num: '03',
    title: 'Export & Apply',
    desc: 'Download as PDF or DOCX, then match jobs and track your application success.',
  },
];

export default function Home() {
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const { showToast } = useToast();
  const [templates, setTemplates] = useState(MOCK_TEMPLATES);
  const [activeTab, setActiveTab] = useState('templates');
  const [publicResumes, setPublicResumes] = useState([]);
  const [usingTemplate, setUsingTemplate] = useState(null);

  useEffect(() => {
    const fetchPublicData = async () => {
      try {
        const [tRes, rRes] = await Promise.all([
          api.get('/templates'),
          api.get('/resumes/public'),
        ]);
        if (tRes.data?.length > 0) setTemplates(tRes.data);
        setPublicResumes(rRes.data || []);
      } catch (_) { }
    };
    fetchPublicData();
  }, []);

  const handleUseTemplate = async (template) => {
    if (!user) {
      navigate('/login');
      return;
    }
    setUsingTemplate(template.templateId);
    try {
      if (template.isPremium) {
        const profileRes = await api.get('/auth/profile');
        if (profileRes.data.subscriptionPlan !== 'PREMIUM' && profileRes.data.role !== 'ADMIN') {
          showToast('This is a premium template. Please upgrade to use it.', 'warning');
          return;
        }
      }

      const res = await api.post('/resumes', {
        title: 'Demo Resume - ' + template.name,
        targetJobTitle: 'Senior Software Engineer',
        templateId: template.templateId,
        language: 'en',
      });
      const newResumeId = res.data.resumeId;

      // Add demo data sections
      const demoSections = [
        { resumeId: newResumeId, sectionType: 'SUMMARY', title: 'Professional Summary', content: 'Results-driven Senior Software Engineer with 6+ years of experience designing and building scalable web applications. Proven expertise in React, Node.js, and cloud architecture. Passionate about writing clean code and mentoring junior developers to improve team velocity.', displayOrder: 0, visible: true },
        { resumeId: newResumeId, sectionType: 'EXPERIENCE', title: 'Work Experience', content: 'Senior Software Engineer | TechCorp Inc.\nJan 2021 - Present\n• Architected and migrated legacy monolith to microservices using Spring Boot and AWS, reducing infrastructure costs by 25%.\n• Led a squad of 5 engineers to deliver a new real-time analytics dashboard used by 10,000+ daily active users.\n• Improved API response times by 40% through Redis caching and query optimization.\n\nSoftware Engineer | StartupX\nJun 2018 - Dec 2020\n• Developed scalable frontend features using React and Redux, improving user retention by 15%.\n• Built and integrated secure payment processing pipelines using Stripe API.', displayOrder: 1, visible: true },
        { resumeId: newResumeId, sectionType: 'SKILLS', title: 'Technical Skills', content: 'Languages: JavaScript, TypeScript, Java, Python, SQL\nFrontend: React, Redux, Next.js, HTML/CSS, Tailwind\nBackend: Node.js, Express, Spring Boot, REST APIs, GraphQL\nDevOps & Cloud: AWS (S3, EC2, Lambda), Docker, Kubernetes, CI/CD, Git\nDatabases: PostgreSQL, MongoDB, Redis', displayOrder: 2, visible: true },
        { resumeId: newResumeId, sectionType: 'EDUCATION', title: 'Education', content: 'Bachelor of Science in Computer Science\nUniversity of Technology | Graduated May 2018\n• GPA: 3.8/4.0\n• Relevant Coursework: Data Structures, Algorithms, Cloud Computing, Database Systems', displayOrder: 3, visible: true }
      ];

      await Promise.all(demoSections.map(sec => api.post('/sections', sec)));

      showToast(`"${template.name}" applied with demo data`, 'success');
      navigate(`/builder/${newResumeId}`);
    } catch (err) {
      const errMsg = err.response?.data?.message || err.response?.data?.error || 'Could not create resume. Try again.';
      showToast(errMsg, 'error');
    } finally {
      setUsingTemplate(null);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)' }}>
      {/* ── Navbar ── */}
      <nav className="navbar">
        <div className="logo">Resume<span>AI</span></div>
        <div className="nav-links">
          {user ? (
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/dashboard')}>
              Go to Dashboard
            </button>
          ) : (
            <>
              <button className="nav-link" onClick={() => navigate('/login')}>Sign In</button>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
                Get Started Free
              </button>
            </>
          )}
        </div>
      </nav>

      {/* ── Hero ── */}
      <section style={{
        maxWidth: 1100,
        margin: '0 auto',
        padding: '96px 40px 72px',
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: 64,
        alignItems: 'center',
      }}>
        <div>
          <h1 style={{ fontSize: 54, fontWeight: 900, letterSpacing: '-0.05em', lineHeight: 1.08, marginBottom: 24 }}>
            Build a resume<br />
            <span style={{ color: 'var(--primary)' }}>that gets hired.</span>
          </h1>
          <p style={{ fontSize: 17, lineHeight: 1.7, color: 'var(--text-muted)', maxWidth: 440, marginBottom: 36 }}>
            Professional templates, AI-generated content, and real-time ATS scoring — everything you need to land your next role.
          </p>
          <div style={{ display: 'flex', gap: 12 }}>
            <button className="btn btn-primary btn-lg" onClick={() => navigate(user ? '/dashboard' : '/login')}>
              Start Building Free
            </button>
            <button className="btn btn-outline btn-lg" onClick={() => document.getElementById('templates-section')?.scrollIntoView({ behavior: 'smooth' })}>
              Browse Templates
            </button>
          </div>
        </div>

        {/* Hero Visual */}
        <div style={{ position: 'relative' }}>
          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            padding: 28,
            boxShadow: 'var(--shadow-lg)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 20 }}>
              <div style={{ width: 10, height: 10, borderRadius: '50%', background: '#ff5f57' }}></div>
              <div style={{ width: 10, height: 10, borderRadius: '50%', background: '#febc2e' }}></div>
              <div style={{ width: 10, height: 10, borderRadius: '50%', background: '#28c840' }}></div>
              <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--text-muted)', fontWeight: 500 }}>resume-preview.pdf</span>
            </div>
            <div style={{ borderBottom: '2px solid var(--primary)', paddingBottom: 16, marginBottom: 16 }}>
              <div style={{ fontSize: 20, fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.03em' }}>Arman Ahmed</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 2 }}>Senior Software Engineer</div>
            </div>
            {['Experience', 'Education', 'Skills'].map((s) => (
              <div key={s} style={{ marginBottom: 14 }}>
                <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--primary)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 6 }}>{s}</div>
                <div style={{ height: 8, background: 'var(--bg)', borderRadius: 4, width: s === 'Skills' ? '70%' : '100%', marginBottom: 5 }}></div>
                <div style={{ height: 8, background: 'var(--bg)', borderRadius: 4, width: s === 'Experience' ? '85%' : '60%' }}></div>
              </div>
            ))}
            <div style={{ marginTop: 18, padding: '10px 14px', background: 'var(--primary-light)', borderRadius: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--primary)' }}>ATS Score</span>
              <span style={{ fontSize: 18, fontWeight: 800, color: 'var(--primary)' }}>89%</span>
            </div>
          </div>
        </div>
      </section>




      {/* ── Templates Section ── */}
      <section id="templates-section" style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 40px 48px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 40 }}>
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--primary)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 10 }}>Template Gallery</div>
            <h2 style={{ fontSize: 36, fontWeight: 800, letterSpacing: '-0.04em', marginBottom: 10 }}>Pick your template</h2>
            <p style={{ fontSize: 15, color: 'var(--text-muted)', maxWidth: 480 }}>
              Every template is ATS-optimised and crafted to pass screening software while still looking great to human readers.
            </p>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className={`btn ${activeTab === 'templates' ? 'btn-primary' : 'btn-outline'} btn-sm`}
              onClick={() => setActiveTab('templates')}
            >Templates</button>
            <button
              className={`btn ${activeTab === 'resumes' ? 'btn-primary' : 'btn-outline'} btn-sm`}
              onClick={() => setActiveTab('resumes')}
            >Public Resumes</button>
          </div>
        </div>

        {activeTab === 'templates' && (
          <div className="resume-grid">
            {templates.map((t) => {
              const color = CATEGORY_COLORS[t.category] || 'var(--primary)';
              const isLoading = usingTemplate === t.templateId;
              return (
                <div key={t.templateId} className="template-card" onClick={() => handleUseTemplate(t)}>
                  <div className="template-thumbnail" style={{ background: '#f1f3f9' }}>
                    {/* Abstract template visual */}
                    <div style={{ width: '70%', padding: '16px 20px', background: 'white', borderRadius: 8, boxShadow: '0 4px 16px rgba(0,0,0,.1)' }}>
                      <div style={{ height: 10, background: color, borderRadius: 3, marginBottom: 10, width: '60%' }}></div>
                      <div style={{ height: 6, background: '#e8eaed', borderRadius: 3, marginBottom: 6 }}></div>
                      <div style={{ height: 6, background: '#e8eaed', borderRadius: 3, width: '75%', marginBottom: 14 }}></div>
                      <div style={{ height: 4, background: color, borderRadius: 2, opacity: 0.3, marginBottom: 8 }}></div>
                      <div style={{ height: 5, background: '#e8eaed', borderRadius: 3, marginBottom: 5 }}></div>
                      <div style={{ height: 5, background: '#e8eaed', borderRadius: 3, width: '80%' }}></div>
                    </div>
                    <div className="template-overlay">
                      <button className="btn btn-primary btn-sm" style={{ pointerEvents: 'none' }}>
                        {isLoading ? <span className="spinner"></span> : 'Use Template'}
                      </button>
                    </div>
                  </div>
                  <div className="template-body">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <div className="template-name">{t.name}</div>
                        <div className="template-cat">{t.category}</div>
                      </div>
                      <span className={`badge ${t.isPremium ? 'badge-yellow' : 'badge-green'}`}>
                        {t.isPremium ? 'Premium' : 'Free'}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {activeTab === 'resumes' && (
          <div className="resume-grid">
            {publicResumes.length === 0 ? (
              <div className="empty-state" style={{ gridColumn: '1/-1' }}>
                <div className="empty-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
                </div>
                <h3>No public resumes yet</h3>
                <p>Be the first to share your resume with the community.</p>
              </div>
            ) : publicResumes.map((r) => (
              <div key={r.resumeId} className="resume-card">
                <h3 style={{ fontSize: 15, marginBottom: 4 }}>{r.title}</h3>
                <p style={{ fontSize: 13 }}>{r.targetJobTitle}</p>
                <button className="btn btn-outline btn-sm" style={{ marginTop: 'auto', paddingTop: 16 }}>View Resume</button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── How it Works ── */}
      <section style={{ background: 'var(--surface)', borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 40px' }}>
          <div style={{ textAlign: 'center', marginBottom: 56 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--primary)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 10 }}>Process</div>
            <h2 style={{ fontSize: 36, fontWeight: 800, letterSpacing: '-0.04em' }}>How it works</h2>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 40 }}>
            {STEPS.map((step) => (
              <div key={step.num}>
                <div style={{ fontSize: 40, fontWeight: 900, color: 'var(--primary-light)', letterSpacing: '-0.06em', marginBottom: 14, lineHeight: 1, border: '1px solid var(--border)', borderRadius: 10, width: 56, height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg)' }}>
                  <span style={{ fontSize: 16, fontWeight: 800, color: 'var(--primary)' }}>{step.num}</span>
                </div>
                <h3 style={{ marginBottom: 10, fontSize: 17 }}>{step.title}</h3>
                <p style={{ fontSize: 14 }}>{step.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 40px' }}>
        <div style={{
          background: 'var(--primary)',
          borderRadius: 20,
          padding: '64px 60px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 40,
        }}>
          <div>
            <h2 style={{ fontSize: 34, fontWeight: 800, color: 'white', letterSpacing: '-0.04em', marginBottom: 10 }}>
              Ready to build your resume?
            </h2>
            <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: 16, maxWidth: 440 }}>
              Build, score, and export a professional resume in minutes — completely free to get started.
            </p>
          </div>
          <button
            className="btn btn-lg"
            style={{ background: 'white', color: 'var(--primary)', fontWeight: 700, flexShrink: 0, borderColor: 'white' }}
            onClick={() => navigate(user ? '/dashboard' : '/login')}
          >
            {user ? 'Go to Dashboard' : 'Create Free Account'}
          </button>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer style={{ borderTop: '1px solid var(--border)', padding: '24px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', maxWidth: 1100, margin: '0 auto' }}>
        <div className="logo" style={{ fontSize: 14 }}>Resume<span>AI</span></div>
        <p style={{ fontSize: 13, margin: 0 }}>Built for job seekers. Powered by AI.</p>
      </footer>
    </div>
  );
}
