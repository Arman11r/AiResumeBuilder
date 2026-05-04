import React, { useState, useEffect, useRef, useCallback } from 'react';
import api from '../services/api';

/* ── Icons ── */
const BellIcon = () => (
  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
  </svg>
);

const CheckIcon = () => (
  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <polyline points="20 6 9 17 4 12"/>
  </svg>
);

const AllReadIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <polyline points="20 6 9 17 4 12"/><polyline points="20 10 9 21 4 16"/>
  </svg>
);

/* ── Type → icon/colour map ── */
const TYPE_META = {
  RESUME_CREATED:         { emoji: '📄', color: '#3b5bdb' },
  RESUME_DELETED:         { emoji: '🗑️', color: '#dc2626' },
  SECTION_ADDED:          { emoji: '➕', color: '#3b5bdb' },
  SECTION_DELETED:        { emoji: '➖', color: '#d97706' },
  AI_CONTENT_GENERATED:   { emoji: '✨', color: '#7c3aed' },
  ATS_COMPLETE:           { emoji: '📊', color: '#0d9c6e' },
  EXPORT_READY:           { emoji: '📥', color: '#0d9c6e' },
  COVER_LETTER_GENERATED: { emoji: '✉️', color: '#7c3aed' },
  JOB_SEARCH_COMPLETE:    { emoji: '🔍', color: '#3b5bdb' },
  JOB_BOOKMARKED:         { emoji: '🔖', color: '#d97706' },
  PROFILE_UPDATED:        { emoji: '👤', color: '#3b5bdb' },
  PASSWORD_CHANGED:       { emoji: '🔐', color: '#dc2626' },
  PLAN_UPGRADED:          { emoji: '⭐', color: '#f59e0b' },
  USER_LOGIN:             { emoji: '👋', color: '#0d9c6e' },
  USER_REGISTERED:        { emoji: '🎉', color: '#7c3aed' },
  SYSTEM_ALERT:           { emoji: '🔔', color: '#3b5bdb' },
};

const getTypeMeta = (type) => TYPE_META[type] || { emoji: '🔔', color: '#6b7280' };

const formatTime = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  const now = new Date();
  const diff = (now - d) / 1000;
  if (diff < 60)   return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

export default function NotificationDropdown({ userId }) {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);

  const fetchNotifications = useCallback(async () => {
    if (!userId) return;
    try {
      const [listRes, countRes] = await Promise.all([
        api.get(`/notifications/${userId}`),
        api.get(`/notifications/${userId}/unread-count`),
      ]);
      setNotifications(listRes.data || []);
      setUnreadCount(countRes.data?.unreadCount || 0);
    } catch (_) {}
  }, [userId]);

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 10000);
    return () => clearInterval(interval);
  }, [fetchNotifications]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const markAsRead = async (id, e) => {
    e.stopPropagation();
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications(n => n.map(x => x.notificationId === id ? { ...x, read: true } : x));
      setUnreadCount(c => Math.max(0, c - 1));
    } catch (_) {}
  };

  const markAllRead = async () => {
    setLoading(true);
    try {
      await api.put(`/notifications/recipient/${userId}/read-all`);
      setNotifications(n => n.map(x => ({ ...x, read: true })));
      setUnreadCount(0);
    } catch (_) {}
    finally { setLoading(false); }
  };

  return (
    <div style={{ position: 'relative' }} ref={dropdownRef}>
      {/* Bell Button */}
      <button
        id="notification-bell-btn"
        className="nav-link"
        onClick={() => setIsOpen(!isOpen)}
        style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 6, padding: '6px 10px' }}
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
      >
        <BellIcon />
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute',
            top: 2, right: 2,
            background: '#dc2626',
            color: 'white',
            borderRadius: '50%',
            width: 16, height: 16,
            fontSize: 9.5,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 700,
            border: '1.5px solid var(--surface)',
            animation: 'bellPulse 2s ease-in-out infinite',
          }}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div
          id="notification-dropdown"
          className="notification-dropdown fade-in"
          role="dialog"
          aria-label="Notifications panel"
        >
          {/* Header */}
          <div style={{
            padding: '14px 16px 10px',
            borderBottom: '1px solid var(--border)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text-main)' }}>
                Notifications
              </span>
              {unreadCount > 0 && (
                <span style={{
                  background: '#dc2626', color: 'white',
                  borderRadius: 999, padding: '1px 7px',
                  fontSize: 11, fontWeight: 700,
                }}>
                  {unreadCount}
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                className="btn btn-ghost btn-sm"
                style={{ fontSize: 11.5, padding: '3px 8px', color: 'var(--primary)', gap: 4 }}
                onClick={markAllRead}
                disabled={loading}
              >
                <AllReadIcon /> Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div style={{ maxHeight: 400, overflowY: 'auto' }}>
            {notifications.length === 0 ? (
              <div style={{ padding: '40px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                <div style={{ fontSize: 28, marginBottom: 8 }}>🔔</div>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>No notifications yet</div>
                <div style={{ fontSize: 12 }}>Activity across your account will appear here.</div>
              </div>
            ) : notifications.map((n) => {
              const meta = getTypeMeta(n.type);
              return (
                <div
                  key={n.notificationId}
                  style={{
                    padding: '12px 14px',
                    borderBottom: '1px solid var(--border)',
                    background: n.read ? 'transparent' : 'rgba(59,91,219,0.04)',
                    display: 'flex',
                    gap: 10,
                    alignItems: 'flex-start',
                    transition: 'background 0.15s',
                    position: 'relative',
                  }}
                >
                  {/* Left accent bar for unread */}
                  {!n.read && (
                    <div style={{
                      position: 'absolute',
                      left: 0, top: 0, bottom: 0,
                      width: 3,
                      background: meta.color,
                      borderRadius: '0 2px 2px 0',
                    }} />
                  )}

                  {/* Icon bubble */}
                  <div style={{
                    width: 32, height: 32,
                    borderRadius: 8,
                    background: n.read ? 'var(--bg)' : `${meta.color}15`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexShrink: 0,
                    fontSize: 14,
                    border: `1px solid ${n.read ? 'var(--border)' : meta.color + '30'}`,
                  }}>
                    {meta.emoji}
                  </div>

                  {/* Content */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      fontSize: 10.5,
                      fontWeight: 700,
                      color: n.read ? 'var(--text-muted)' : meta.color,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      marginBottom: 2,
                    }}>
                      {(n.type || '').replace(/_/g, ' ')}
                    </div>
                    <p style={{
                      margin: 0,
                      fontSize: 12.5,
                      color: n.read ? 'var(--text-muted)' : 'var(--text-secondary)',
                      lineHeight: 1.5,
                    }}>
                      {n.message}
                    </p>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>
                      {formatTime(n.createdAt)}
                    </div>
                  </div>

                  {/* Mark-read button */}
                  {!n.read && (
                    <button
                      className="btn btn-ghost btn-sm"
                      style={{ padding: '4px 6px', color: meta.color, flexShrink: 0, opacity: 0.8 }}
                      onClick={(e) => markAsRead(n.notificationId, e)}
                      title="Mark as read"
                    >
                      <CheckIcon />
                    </button>
                  )}
                </div>
              );
            })}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div style={{
              padding: '10px 16px',
              borderTop: '1px solid var(--border)',
              textAlign: 'center',
              fontSize: 12,
              color: 'var(--text-muted)',
              background: 'var(--bg)',
            }}>
              Showing {notifications.length} notification{notifications.length !== 1 ? 's' : ''}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
