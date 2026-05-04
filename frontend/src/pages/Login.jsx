import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import api from '../services/api';
import { pushNotification } from '../services/notify';

const FEATURES = [
  { title: 'AI-Powered Content', desc: 'Generate professional summaries, bullet points, and skills tailored to your target role.' },
  { title: 'Real-Time ATS Scoring', desc: 'See exactly how your resume scores against job descriptions before you apply.' },
  { title: 'Job Match', desc: 'Fetch live listings from LinkedIn and Naukri, and get a fit score for each role.' },
];

export default function Login() {
  const { login, register } = useContext(AuthContext);
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isLogin) {
        await login(email, password);
        showToast('Welcome back!', 'success');
        const uid = localStorage.getItem('userId');
        if (uid) pushNotification({
          recipientId: uid,
          type: 'USER_LOGIN',
          message: `Welcome back! You signed in successfully on ${new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}.`,
        });
      } else {
        await register({ fullName: name, email, password, phone: '9999999999' });
        showToast('Account created! Welcome to ResumeAI 🎉', 'success');
        const uid = localStorage.getItem('userId');
        if (uid) pushNotification({
          recipientId: uid,
          type: 'USER_REGISTERED',
          message: `Welcome to ResumeAI, ${name}! Your account is ready. Start by creating your first resume from the dashboard.`,
        });
      }
      navigate('/dashboard');
    } catch (err) {
      showToast(err.response?.data?.message || 'Authentication failed', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      const res = await api.post('/auth/google', { token: credentialResponse.credential });
      const { token, userId, email: gEmail, role } = res.data;
      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('email', gEmail);
      localStorage.setItem('role', role || 'USER');
      window.location.href = '/dashboard';
    } catch (err) {
      showToast('Google sign-in failed', 'error');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      background: 'var(--bg)',
    }}>
      {/* ── Left Panel — Brand ── */}
      <div style={{
        background: 'var(--surface)',
        borderRight: '1px solid var(--border)',
        padding: '48px 56px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}>
        {/* Logo */}
        <div className="logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
          Resume<span>AI</span>
        </div>

        {/* Value Props */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', paddingTop: 40 }}>
          <div style={{
            fontSize: 11,
            fontWeight: 700,
            color: 'var(--primary)',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            marginBottom: 20,
          }}>Why ResumeAI</div>
          <h1 style={{ fontSize: 38, fontWeight: 900, letterSpacing: '-0.05em', lineHeight: 1.1, marginBottom: 40, color: 'var(--text-main)' }}>
            Land your next job<br />with AI on your side.
          </h1>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
            {FEATURES.map((f, i) => (
              <div key={i} style={{ display: 'flex', gap: 16 }}>
                <div style={{
                  width: 36,
                  height: 36,
                  borderRadius: 8,
                  background: 'var(--primary-light)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                  fontWeight: 800,
                  fontSize: 13,
                  color: 'var(--primary)',
                }}>
                  {String(i + 1).padStart(2, '0')}
                </div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-main)', marginBottom: 4 }}>{f.title}</div>
                  <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.55 }}>{f.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>


      </div>

      {/* ── Right Panel — Form ── */}
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 56px',
      }}>
        <div style={{ width: '100%', maxWidth: 400 }} className="fade-in">
          {/* Tab Toggle */}
          <div style={{
            display: 'flex',
            background: 'var(--bg)',
            border: '1px solid var(--border)',
            borderRadius: 10,
            padding: 4,
            marginBottom: 32,
          }}>
            {['Sign In', 'Create Account'].map((label, i) => (
              <button
                key={label}
                onClick={() => setIsLogin(i === 0)}
                style={{
                  flex: 1,
                  padding: '9px 0',
                  borderRadius: 7,
                  border: 'none',
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                  fontSize: 14,
                  fontWeight: 600,
                  transition: 'all 0.15s',
                  background: (isLogin && i === 0) || (!isLogin && i === 1) ? 'var(--surface)' : 'transparent',
                  color: (isLogin && i === 0) || (!isLogin && i === 1) ? 'var(--text-main)' : 'var(--text-muted)',
                  boxShadow: (isLogin && i === 0) || (!isLogin && i === 1) ? 'var(--shadow-sm)' : 'none',
                }}
              >
                {label}
              </button>
            ))}
          </div>

          <h2 style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.03em', marginBottom: 6 }}>
            {isLogin ? 'Welcome back' : 'Create your account'}
          </h2>
          <p style={{ fontSize: 14, marginBottom: 28 }}>
            {isLogin
              ? 'Sign in to continue building your resume.'
              : 'Free forever. No credit card required.'}
          </p>

          <form onSubmit={handleSubmit}>
            {!isLogin && (
              <div className="form-group">
                <label>Full Name</label>
                <input
                  className="input-field"
                  type="text"
                  placeholder="Alex Johnson"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
            )}
            <div className="form-group">
              <label>Email address</label>
              <input
                className="input-field"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="form-group" style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <label style={{ margin: 0 }}>Password</label>
                {isLogin && (
                  <span style={{ fontSize: 12, color: 'var(--primary)', cursor: 'pointer' }}>
                    Forgot password?
                  </span>
                )}
              </div>
              <input
                className="input-field"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button
              className="btn btn-primary"
              style={{ width: '100%', padding: '11px', fontSize: 15, fontWeight: 600 }}
              disabled={loading}
            >
              {loading
                ? <span className="spinner"></span>
                : isLogin ? 'Sign In' : 'Create Account'}
            </button>
          </form>

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0' }}>
            <div style={{ flex: 1, height: 1, background: 'var(--border)' }}></div>
            <span style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 500 }}>OR</span>
            <div style={{ flex: 1, height: 1, background: 'var(--border)' }}></div>
          </div>

          {/* Google Login */}
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => showToast('Google sign-in failed', 'error')}
              width="400"
            />
          </div>

          <p style={{ textAlign: 'center', fontSize: 13, marginTop: 24 }}>
            {isLogin ? "Don't have an account? " : "Already have an account? "}
            <span
              style={{ color: 'var(--primary)', cursor: 'pointer', fontWeight: 600 }}
              onClick={() => setIsLogin(!isLogin)}
            >
              {isLogin ? 'Sign up free' : 'Sign in'}
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}
