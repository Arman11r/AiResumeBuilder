import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import api from '../services/api';

export default function QuotaWidget() {
  const { user } = useContext(AuthContext);
  const [quota, setQuota] = useState(null);

  useEffect(() => {
    if (user?.userId) {
      api.get(`/ai/quota/${user.userId}`)
        .then(res => setQuota(res.data))
        .catch(() => {});
    }
  }, [user]);

  if (!quota) return null;

  if (quota.isPremium) {
    return (
      <div style={{ padding: '12px 16px', background: 'var(--success)', color: 'white', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>
        Premium Plan: Unlimited AI Requests
      </div>
    );
  }

  return (
    <div style={{ padding: '12px 16px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8, marginTop: 16 }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 10 }}>
        Free Daily Quota
      </div>
      
      <div style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
          <span>AI Content Generation</span>
          <span style={{ fontWeight: 600, color: quota.contentCallsRemaining === 0 ? 'var(--danger)' : 'var(--text-main)' }}>
            {quota.contentCallsRemaining} left
          </span>
        </div>
        <div style={{ height: 4, background: 'var(--bg)', borderRadius: 99, overflow: 'hidden' }}>
          <div style={{
            height: '100%',
            width: `${(quota.contentCallsUsed / (quota.contentCallsUsed + quota.contentCallsRemaining)) * 100}%`,
            background: quota.contentCallsRemaining === 0 ? 'var(--danger)' : 'var(--primary)',
            borderRadius: 99
          }}></div>
        </div>
      </div>

      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
          <span>ATS Scans</span>
          <span style={{ fontWeight: 600, color: quota.atsCallsRemaining === 0 ? 'var(--danger)' : 'var(--text-main)' }}>
            {quota.atsCallsRemaining} left
          </span>
        </div>
        <div style={{ height: 4, background: 'var(--bg)', borderRadius: 99, overflow: 'hidden' }}>
          <div style={{
            height: '100%',
            width: `${(quota.atsCallsUsed / (quota.atsCallsUsed + quota.atsCallsRemaining)) * 100}%`,
            background: quota.atsCallsRemaining === 0 ? 'var(--danger)' : 'var(--primary)',
            borderRadius: 99
          }}></div>
        </div>
      </div>
    </div>
  );
}
