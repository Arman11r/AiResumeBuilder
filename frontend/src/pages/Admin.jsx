import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { pushNotification } from '../services/notify';

/* ── tiny helpers ── */
const PlanBadge = ({ plan }) => (
  <span style={{
    background: plan === 'PREMIUM' ? '#fef3c7' : '#f1f5f9',
    color: plan === 'PREMIUM' ? '#d97706' : '#64748b',
    padding: '2px 9px', borderRadius: 999, fontSize: 11, fontWeight: 700,
  }}>{plan}</span>
);

const StatusDot = ({ active }) => (
  <span style={{ display:'inline-flex', alignItems:'center', gap:5, fontSize:12, fontWeight:600,
    color: active ? '#0d9c6e' : '#dc2626' }}>
    <span style={{ width:7, height:7, borderRadius:'50%',
      background: active ? '#0d9c6e' : '#dc2626', display:'inline-block' }} />
    {active ? 'Active' : 'Suspended'}
  </span>
);

const StatCard = ({ label, value, sub, color = 'var(--primary)' }) => (
  <div style={{ background:'#fff', border:'1px solid #e4e7ec', borderRadius:14,
    padding:'20px 24px', flex:1 }}>
    <div style={{ fontSize:11, fontWeight:700, color:'#6b7280', textTransform:'uppercase',
      letterSpacing:'0.06em', marginBottom:8 }}>{label}</div>
    <div style={{ fontSize:36, fontWeight:900, color, letterSpacing:'-0.04em',
      lineHeight:1 }}>{value}</div>
    {sub && <div style={{ fontSize:12, color:'#9ca3af', marginTop:6 }}>{sub}</div>}
  </div>
);

export default function Admin() {
  const { user } = useContext(AuthContext);
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [tab, setTab] = useState('users');

  /* users */
  const [users, setUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [editingUser, setEditingUser] = useState(null);

  /* templates */
  const [templates, setTemplates] = useState([]);
  const [showNewTemplate, setShowNewTemplate] = useState(false);
  const [tmpl, setTmpl] = useState({ name:'', description:'', category:'PROFESSIONAL', isPremium:false, htmlLayout:'', cssStyles:'' });

  /* analytics */
  const [stats, setStats] = useState(null);
  const [allResumes, setAllResumes] = useState([]);

  /* broadcast */
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [broadcastTier, setBroadcastTier] = useState('ALL');
  const [broadcasting, setBroadcasting] = useState(false);

  /* ── loaders ── */
  const loadUsers = async () => {
    setUsersLoading(true);
    try { const r = await api.get('/auth/admin/users'); setUsers(r.data || []); }
    catch { showToast('Failed to load users', 'error'); }
    finally { setUsersLoading(false); }
  };

  const loadStats = async () => {
    try {
      const [sRes, tRes, rRes] = await Promise.all([
        api.get('/auth/admin/stats'),
        api.get('/templates'),
        api.get('/resumes/public').catch(() => ({ data: [] })),
      ]);
      // Also get total resume count via all-public + estimate
      setStats({ ...sRes.data, publicResumes: (rRes.data || []).length });
      setTemplates(tRes.data || []);
    } catch (e) {
      showToast('Failed to load analytics', 'error');
    }
  };

  useEffect(() => { loadUsers(); loadStats(); }, []);

  /* ── user actions ── */
  const handlePlanChange = async (u, plan) => {
    try {
      await api.put(`/auth/admin/users/${u.userId}/subscription`, { plan });
      showToast(`${u.email} → ${plan}`, 'success');
      pushNotification({ recipientId: u.userId, type: 'PLAN_UPGRADED',
        message: `An admin updated your subscription to ${plan}.` });
      loadUsers();
    } catch { showToast('Failed to update plan', 'error'); }
  };

  const handleSuspend = async (u, suspend) => {
    try {
      await api.put(`/auth/admin/users/${u.userId}/suspend`, { suspend });
      showToast(suspend ? `${u.email} suspended` : `${u.email} reactivated`, suspend ? 'warning' : 'success');
      loadUsers();
    } catch { showToast('Failed to update status', 'error'); }
  };

  const handleDelete = async (u) => {
    if (!window.confirm(`Permanently delete ${u.email}?`)) return;
    try {
      await api.delete(`/auth/admin/users/${u.userId}`);
      showToast('User deleted', 'success');
      loadUsers();
    } catch { showToast('Failed to delete user', 'error'); }
  };

  /* ── template actions ── */
  const loadTemplates = async () => {
    try { const r = await api.get('/templates'); setTemplates(r.data || []); }
    catch { showToast('Failed to load templates', 'error'); }
  };

  const handleCreateTemplate = async () => {
    if (!tmpl.name || !tmpl.category) return showToast('Name & category required', 'error');
    try {
      await api.post('/templates', tmpl);
      showToast('Template created', 'success');
      setShowNewTemplate(false);
      setTmpl({ name:'', description:'', category:'PROFESSIONAL', isPremium:false, htmlLayout:'', cssStyles:'' });
      loadTemplates();
    } catch { showToast('Failed to create template', 'error'); }
  };

  const handleDeactivateTemplate = async (id) => {
    try {
      await api.put(`/templates/${id}/deactivate`);
      showToast('Template deactivated', 'success');
      loadTemplates();
    } catch { showToast('Failed to deactivate', 'error'); }
  };

  /* ── derived ── */
  const filtered = users.filter(u =>
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.fullName?.toLowerCase().includes(search.toLowerCase())
  );

  const CATEGORIES = ['PROFESSIONAL','CREATIVE','MODERN','MINIMALIST','ATS_OPTIMISED'];

  /* ── broadcast handler ── */
  const handleBroadcast = async () => {
    if (!broadcastMsg.trim()) return showToast('Message cannot be empty', 'error');
    setBroadcasting(true);
    try {
      // Build recipient list from already-loaded users, filtered by tier
      const targets = broadcastTier === 'ALL'
        ? users
        : users.filter(u => u.subscriptionPlan === broadcastTier);

      if (targets.length === 0) {
        showToast('No users found for the selected tier', 'error');
        setBroadcasting(false);
        return;
      }

      await api.post('/notifications/broadcast', {
        title: '📢 Platform Announcement',
        message: broadcastMsg.trim(),
        type: 'ADMIN_BROADCAST',
        recipientIds: targets.map(u => u.userId),
      });
      showToast(`Broadcast sent to ${targets.length} user${targets.length !== 1 ? 's' : ''} ✓`, 'success');
      setBroadcastMsg('');
    } catch { showToast('Broadcast failed', 'error'); }
    finally { setBroadcasting(false); }
  };

  /* ── sidebar tabs ── */
  const TABS = [
    { id:'users',     icon:'👥', label:'User Management' },
    { id:'templates', icon:'📄', label:'Templates' },
    { id:'analytics', icon:'📊', label:'Analytics' },
    { id:'broadcast', icon:'📢', label:'Send Notification' },
  ];

  return (
    <div style={{ minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      {/* Top bar */}
      <nav style={{ background:'#0f172a', borderBottom:'1px solid #1e293b',
        padding:'0 32px', height:56, display:'flex', alignItems:'center',
        justifyContent:'space-between', position:'sticky', top:0, zIndex:100 }}>
        <div style={{ display:'flex', alignItems:'center', gap:24 }}>
          <button onClick={() => navigate('/dashboard')}
            style={{ background:'none', border:'none', color:'#94a3b8',
              fontSize:13, cursor:'pointer', display:'flex', alignItems:'center', gap:6 }}>
            ← Dashboard
          </button>
          <div style={{ width:1, height:20, background:'#1e293b' }} />
          <span style={{ color:'#e2e8f0', fontWeight:700, fontSize:14, letterSpacing:'-0.02em' }}>
            ⚙️ ResumeAI Admin Portal
          </span>
        </div>
        <span style={{ fontSize:12, color:'#64748b' }}>{user?.email}</span>
      </nav>

      <div style={{ display:'flex', flex:1 }}>
        {/* Sidebar */}
        <div style={{ width:220, background:'#1e293b', padding:'20px 0', flexShrink:0 }}>
          {TABS.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              style={{ width:'100%', padding:'11px 20px', background: tab===t.id ? '#334155' : 'transparent',
                color: tab===t.id ? '#e2e8f0' : '#94a3b8', border:'none', cursor:'pointer',
                fontSize:13.5, fontWeight:500, display:'flex', alignItems:'center', gap:10,
                textAlign:'left', fontFamily:'inherit', transition:'background 0.15s' }}>
              {t.icon} {t.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div style={{ flex:1, padding:40, background:'#f8fafc', overflowY:'auto' }}>

          {/* ── USERS ── */}
          {tab === 'users' && (
            <div className="fade-in">
              <div style={{ marginBottom:24 }}>
                <h2 style={{ marginBottom:4 }}>User Management</h2>
                <p style={{ fontSize:13 }}>View, update subscriptions, suspend, or permanently delete accounts.</p>
              </div>

              <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:20 }}>
                <input
                  className="input-field"
                  style={{ maxWidth:320 }}
                  placeholder="Search by name or email…"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                />
                <span style={{ fontSize:13, color:'#6b7280' }}>{filtered.length} users</span>
              </div>

              <div className="card" style={{ padding:0, overflow:'hidden' }}>
                <table style={{ width:'100%', borderCollapse:'collapse', fontSize:13 }}>
                  <thead>
                    <tr style={{ background:'#f8fafc', borderBottom:'1px solid #e4e7ec' }}>
                      {['Name / Email','Role','Plan','Status','Joined','Actions'].map(h => (
                        <th key={h} style={{ padding:'11px 16px', textAlign:'left',
                          fontSize:11, fontWeight:700, color:'#6b7280', textTransform:'uppercase',
                          letterSpacing:'0.05em' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {usersLoading ? (
                      <tr><td colSpan={6} style={{ padding:40, textAlign:'center' }}>
                        <div className="spinner spinner-dark" style={{ margin:'0 auto' }} />
                      </td></tr>
                    ) : filtered.map(u => (
                      <tr key={u.userId} style={{ borderBottom:'1px solid #f1f5f9',
                        background: u.userId === editingUser ? '#eff6ff' : '#fff' }}>
                        <td style={{ padding:'12px 16px' }}>
                          <div style={{ fontWeight:600, color:'#0d1117' }}>{u.fullName || '—'}</div>
                          <div style={{ fontSize:12, color:'#6b7280' }}>{u.email}</div>
                        </td>
                        <td style={{ padding:'12px 16px' }}>
                          <span style={{ fontSize:11, fontWeight:700,
                            color: u.role==='ADMIN' ? '#7c3aed' : '#374151',
                            background: u.role==='ADMIN' ? '#ede9fe' : '#f3f4f6',
                            padding:'2px 8px', borderRadius:999 }}>{u.role}</span>
                        </td>
                        <td style={{ padding:'12px 16px' }}><PlanBadge plan={u.subscriptionPlan} /></td>
                        <td style={{ padding:'12px 16px' }}><StatusDot active={u.active} /></td>
                        <td style={{ padding:'12px 16px', color:'#6b7280' }}>
                          {u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-US',{month:'short',day:'numeric',year:'2-digit'}) : '—'}
                        </td>
                        <td style={{ padding:'12px 16px' }}>
                          <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
                            <select
                              value={u.subscriptionPlan}
                              onChange={e => handlePlanChange(u, e.target.value)}
                              style={{ fontSize:11, padding:'3px 6px', borderRadius:6,
                                border:'1px solid #d1d5db', cursor:'pointer', fontFamily:'inherit' }}>
                              <option value="FREE">FREE</option>
                              <option value="PREMIUM">PREMIUM</option>
                            </select>
                            <button
                              onClick={() => handleSuspend(u, u.active)}
                              className="btn btn-sm"
                              style={{ fontSize:11, padding:'3px 8px',
                                background: u.active ? '#fef3c7' : '#d1fae5',
                                color: u.active ? '#d97706' : '#059669',
                                border: `1px solid ${u.active ? '#fcd34d' : '#6ee7b7'}` }}>
                              {u.active ? 'Suspend' : 'Reactivate'}
                            </button>
                            {u.role !== 'ADMIN' && (
                              <button onClick={() => handleDelete(u)}
                                className="btn btn-danger btn-sm"
                                style={{ fontSize:11, padding:'3px 8px' }}>
                                Delete
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ── TEMPLATES ── */}
          {tab === 'templates' && (
            <div className="fade-in">
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:24 }}>
                <div>
                  <h2 style={{ marginBottom:4 }}>Template Management</h2>
                  <p style={{ fontSize:13 }}>Create, edit, or deactivate resume templates including HTML/CSS layouts.</p>
                </div>
                <button className="btn btn-primary" onClick={() => setShowNewTemplate(!showNewTemplate)}>
                  {showNewTemplate ? '✕ Cancel' : '+ New Template'}
                </button>
              </div>

              {showNewTemplate && (
                <div className="card fade-in" style={{ marginBottom:24, borderLeft:'3px solid var(--primary)' }}>
                  <h3 style={{ marginBottom:16 }}>Create New Template</h3>
                  <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:16 }}>
                    <div className="form-group" style={{ margin:0 }}>
                      <label>Template Name *</label>
                      <input className="input-field" value={tmpl.name}
                        onChange={e => setTmpl({...tmpl, name:e.target.value})}
                        placeholder="e.g. Modern Blue" />
                    </div>
                    <div className="form-group" style={{ margin:0 }}>
                      <label>Category *</label>
                      <select className="input-field" value={tmpl.category}
                        onChange={e => setTmpl({...tmpl, category:e.target.value})}>
                        {CATEGORIES.map(c => <option key={c} value={c}>{c.replace('_',' ')}</option>)}
                      </select>
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Description</label>
                    <input className="input-field" value={tmpl.description}
                      onChange={e => setTmpl({...tmpl, description:e.target.value})}
                      placeholder="Brief description of the template" />
                  </div>
                  <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:16 }}>
                    <div className="form-group" style={{ margin:0 }}>
                      <label>HTML Layout</label>
                      <textarea className="input-field" rows={5} value={tmpl.htmlLayout}
                        onChange={e => setTmpl({...tmpl, htmlLayout:e.target.value})}
                        placeholder="Paste HTML template code…" style={{ fontFamily:'monospace', fontSize:12 }} />
                    </div>
                    <div className="form-group" style={{ margin:0 }}>
                      <label>CSS Styles</label>
                      <textarea className="input-field" rows={5} value={tmpl.cssStyles}
                        onChange={e => setTmpl({...tmpl, cssStyles:e.target.value})}
                        placeholder="Paste CSS styles…" style={{ fontFamily:'monospace', fontSize:12 }} />
                    </div>
                  </div>
                  <div style={{ display:'flex', alignItems:'center', gap:16 }}>
                    <label style={{ display:'flex', alignItems:'center', gap:8, fontSize:13, cursor:'pointer' }}>
                      <input type="checkbox" checked={tmpl.isPremium}
                        onChange={e => setTmpl({...tmpl, isPremium:e.target.checked})} />
                      Premium Template
                    </label>
                    <button className="btn btn-primary" onClick={handleCreateTemplate}>Create Template</button>
                  </div>
                </div>
              )}

              <div className="resume-grid">
                {templates.map(t => (
                  <div key={t.templateId} className="resume-card" style={{ padding:20 }}>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:10 }}>
                      <h3 style={{ margin:0, fontSize:14 }}>{t.name}</h3>
                      <div style={{ display:'flex', gap:6 }}>
                        <PlanBadge plan={t.isPremium ? 'PREMIUM' : 'FREE'} />
                        {!t.isActive && (
                          <span style={{ fontSize:10, fontWeight:700, background:'#fee2e2',
                            color:'#dc2626', padding:'2px 6px', borderRadius:999 }}>INACTIVE</span>
                        )}
                      </div>
                    </div>
                    <p style={{ fontSize:12, color:'#6b7280', margin:'0 0 6px 0' }}>{t.description || '—'}</p>
                    <div style={{ fontSize:11, color:'#94a3b8', marginBottom:12 }}>
                      Category: {t.category} · Used {t.usageCount} times
                    </div>
                    <div style={{ display:'flex', gap:8 }}>
                      {t.isActive && (
                        <button className="btn btn-danger btn-sm"
                          style={{ fontSize:11 }}
                          onClick={() => handleDeactivateTemplate(t.templateId)}>
                          Deactivate
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              {templates.length === 0 && (
                <div className="empty-state"><h3>No templates yet</h3><p>Create your first template above.</p></div>
              )}
            </div>
          )}

          {/* ── ANALYTICS ── */}
          {tab === 'analytics' && (
            <div className="fade-in">
              <div style={{ marginBottom:24 }}>
                <h2 style={{ marginBottom:4 }}>Platform Analytics</h2>
                <p style={{ fontSize:13 }}>Live metrics from all microservices.</p>
              </div>

              {stats ? (
                <>
                  <div style={{ display:'flex', gap:16, marginBottom:24, flexWrap:'wrap' }}>
                    <StatCard label="Total Users" value={stats.totalUsers} sub="all time registrations" color="var(--primary)" />
                    <StatCard label="Active Users" value={stats.activeUsers} sub="not suspended" color="#0d9c6e" />
                    <StatCard label="Premium Users" value={stats.premiumUsers} sub="paid subscribers" color="#d97706" />
                    <StatCard label="Free Users" value={stats.freeUsers} sub="on free plan" color="#6b7280" />
                    <StatCard label="Public Resumes" value={stats.publicResumes} sub="in gallery" color="#8b5cf6" />
                  </div>

                  {/* Conversion bar */}
                  <div className="card" style={{ marginBottom:24 }}>
                    <h3 style={{ marginBottom:16 }}>Subscription Breakdown</h3>
                    <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:8 }}>
                      <span style={{ fontSize:13, color:'#374151', width:80 }}>Premium</span>
                      <div style={{ flex:1, height:12, background:'#f1f5f9', borderRadius:99, overflow:'hidden' }}>
                        <div style={{ width:`${stats.totalUsers > 0 ? (stats.premiumUsers/stats.totalUsers*100).toFixed(1) : 0}%`,
                          height:'100%', background:'linear-gradient(90deg,#d97706,#f59e0b)', borderRadius:99, transition:'width 0.8s ease' }} />
                      </div>
                      <span style={{ fontSize:13, fontWeight:700, color:'#d97706', width:50 }}>
                        {stats.totalUsers > 0 ? (stats.premiumUsers/stats.totalUsers*100).toFixed(1) : 0}%
                      </span>
                    </div>
                    <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                      <span style={{ fontSize:13, color:'#374151', width:80 }}>Free</span>
                      <div style={{ flex:1, height:12, background:'#f1f5f9', borderRadius:99, overflow:'hidden' }}>
                        <div style={{ width:`${stats.totalUsers > 0 ? (stats.freeUsers/stats.totalUsers*100).toFixed(1) : 0}%`,
                          height:'100%', background:'linear-gradient(90deg,#3b5bdb,#6366f1)', borderRadius:99, transition:'width 0.8s ease' }} />
                      </div>
                      <span style={{ fontSize:13, fontWeight:700, color:'var(--primary)', width:50 }}>
                        {stats.totalUsers > 0 ? (stats.freeUsers/stats.totalUsers*100).toFixed(1) : 0}%
                      </span>
                    </div>
                  </div>

                  {/* Templates summary */}
                  <div style={{ display:'flex', gap:16 }}>
                    <div className="card" style={{ flex:1 }}>
                      <h3 style={{ marginBottom:16 }}>Template Stats</h3>
                      <div style={{ display:'flex', gap:24 }}>
                        <div><div style={{ fontSize:28, fontWeight:800, color:'var(--primary)' }}>{templates.length}</div>
                          <div style={{ fontSize:12, color:'#6b7280' }}>Total Templates</div></div>
                        <div><div style={{ fontSize:28, fontWeight:800, color:'#0d9c6e' }}>{templates.filter(t=>t.isActive).length}</div>
                          <div style={{ fontSize:12, color:'#6b7280' }}>Active</div></div>
                        <div><div style={{ fontSize:28, fontWeight:800, color:'#d97706' }}>{templates.filter(t=>t.isPremium).length}</div>
                          <div style={{ fontSize:12, color:'#6b7280' }}>Premium</div></div>
                        <div><div style={{ fontSize:28, fontWeight:800, color:'#6b7280' }}>{templates.reduce((a,t)=>a+(t.usageCount||0),0)}</div>
                          <div style={{ fontSize:12, color:'#6b7280' }}>Total Uses</div></div>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div style={{ textAlign:'center', padding:64 }}>
                  <div className="spinner spinner-dark" style={{ margin:'0 auto 16px' }} />
                  <p>Loading analytics…</p>
                </div>
              )}
            </div>
          )}
          {/* ── BROADCAST ── */}
          {tab === 'broadcast' && (
            <div className="fade-in">
              <div style={{ marginBottom:24 }}>
                <h2 style={{ marginBottom:4 }}>Send Platform Notification</h2>
                <p style={{ fontSize:13 }}>Broadcast an alert to all users or filter by subscription tier.</p>
              </div>

              <div className="card" style={{ maxWidth: 600 }}>
                <div className="form-group">
                  <label>Target Audience</label>
                  <select
                    className="input-field"
                    value={broadcastTier}
                    onChange={e => setBroadcastTier(e.target.value)}
                  >
                    <option value="ALL">All Users</option>
                    <option value="FREE">Free Users Only</option>
                    <option value="PREMIUM">Premium Users Only</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Notification Message</label>
                  <textarea
                    className="input-field"
                    rows={5}
                    value={broadcastMsg}
                    onChange={e => setBroadcastMsg(e.target.value)}
                    placeholder="Type your broadcast message here… e.g. 'We just released a new feature! Check it out in your dashboard.'"
                  />
                  <div style={{ fontSize: 11, color: '#6b7280', marginTop: 4 }}>{broadcastMsg.length}/500 characters</div>
                </div>

                <div style={{ display:'flex', alignItems:'center', gap:16 }}>
                  <button
                    className="btn btn-primary"
                    onClick={handleBroadcast}
                    disabled={broadcasting || !broadcastMsg.trim()}
                  >
                    {broadcasting ? <><span className="spinner" /> Sending…</> : '📢 Send Broadcast'}
                  </button>
                  <span style={{ fontSize: 12, color: '#6b7280' }}>
                    This will send an in-app notification to {broadcastTier === 'ALL' ? 'all users' : `${broadcastTier} users`}.
                  </span>
                </div>
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
