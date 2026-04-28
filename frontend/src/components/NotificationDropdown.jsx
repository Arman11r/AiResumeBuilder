import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';

const BellIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
  </svg>
);

const CheckIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <polyline points="20 6 9 17 4 12"/>
  </svg>
);

export default function NotificationDropdown({ userId }) {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const dropdownRef = useRef(null);

  const fetchNotifications = async () => {
    try {
      const [listRes, countRes] = await Promise.all([
        api.get(`/notifications/${userId}`),
        api.get(`/notifications/${userId}/unread-count`),
      ]);
      setNotifications(listRes.data || []);
      setUnreadCount(countRes.data?.unreadCount || 0);
    } catch (_) {}
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 10000);
    return () => clearInterval(interval);
  }, [userId]);

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
      fetchNotifications();
    } catch (_) {}
  };

  const markAllRead = async () => {
    try {
      await api.put(`/notifications/recipient/${userId}/read-all`);
      fetchNotifications();
    } catch (_) {}
  };

  return (
    <div style={{ position: 'relative' }} ref={dropdownRef}>
      <button
        className="nav-link"
        onClick={() => setIsOpen(!isOpen)}
        style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 6 }}
      >
        <BellIcon />
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute',
            top: 2, right: 2,
            background: 'var(--danger)',
            color: 'white',
            borderRadius: '50%',
            width: 15, height: 15,
            fontSize: 9,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            border: '1.5px solid var(--surface)',
          }}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="notification-dropdown fade-in">
          {/* Header */}
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}>
            <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-main)' }}>
              Notifications {unreadCount > 0 && <span className="badge badge-blue" style={{ marginLeft: 6 }}>{unreadCount}</span>}
            </span>
            {unreadCount > 0 && (
              <button
                className="btn btn-ghost btn-sm"
                style={{ fontSize: 11, padding: '3px 8px', color: 'var(--primary)' }}
                onClick={markAllRead}
              >
                Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div style={{ maxHeight: 360, overflowY: 'auto' }}>
            {notifications.length === 0 ? (
              <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                No notifications yet
              </div>
            ) : notifications.map((n) => (
              <div
                key={n.notificationId}
                style={{
                  padding: '12px 16px',
                  borderBottom: '1px solid var(--border)',
                  background: n.read ? 'var(--surface)' : 'var(--primary-light)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  gap: 10,
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{
                    fontSize: 11,
                    fontWeight: 700,
                    color: n.read ? 'var(--text-muted)' : 'var(--primary)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.05em',
                    marginBottom: 3,
                  }}>
                    {n.type.replace(/_/g, ' ')}
                  </div>
                  <p style={{ margin: 0, fontSize: 12.5, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                    {n.message}
                  </p>
                </div>
                {!n.read && (
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ padding: '4px 6px', color: 'var(--primary)', flexShrink: 0 }}
                    onClick={(e) => markAsRead(n.notificationId, e)}
                    title="Mark as read"
                  >
                    <CheckIcon />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
