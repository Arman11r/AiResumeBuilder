import React from 'react';

export default function ResumePreview({ resume, sections }) {
    if (!resume) return null;

    const summarySection = sections.find(s => s.sectionType === 'SUMMARY');
    const expSections = sections.filter(s => s.sectionType === 'EXPERIENCE').sort((a,b)=>a.displayOrder-b.displayOrder);
    const eduSections = sections.filter(s => s.sectionType === 'EDUCATION').sort((a,b)=>a.displayOrder-b.displayOrder);
    const skillSections = sections.filter(s => s.sectionType === 'SKILLS').sort((a,b)=>a.displayOrder-b.displayOrder);
    const otherSections = sections.filter(s => !['SUMMARY','EXPERIENCE','EDUCATION','SKILLS'].includes(s.sectionType)).sort((a,b)=>a.displayOrder-b.displayOrder);

    return (
        <div className="resume-paper fade-in">
            <h1 className="preview-name">{resume.title || 'Your Name'}</h1>
            <div className="preview-title">{resume.targetJobTitle || 'Target Role'}</div>
            
            {summarySection && summarySection.visible && (
                <div>
                    <div className="preview-section-title">{summarySection.title || 'Summary'}</div>
                    <div className="preview-content">{summarySection.content}</div>
                </div>
            )}

            {expSections.length > 0 && (
                <div>
                    <div className="preview-section-title">Experience</div>
                    {expSections.map(s => s.visible && (
                        <div key={s.sectionId} style={{ marginBottom: 12 }}>
                            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{s.title}</div>
                            <div className="preview-content">{s.content}</div>
                        </div>
                    ))}
                </div>
            )}

            {eduSections.length > 0 && (
                <div>
                    <div className="preview-section-title">Education</div>
                    {eduSections.map(s => s.visible && (
                        <div key={s.sectionId} style={{ marginBottom: 12 }}>
                            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{s.title}</div>
                            <div className="preview-content">{s.content}</div>
                        </div>
                    ))}
                </div>
            )}

            {skillSections.length > 0 && (
                <div>
                    <div className="preview-section-title">Skills</div>
                    {skillSections.map(s => s.visible && (
                        <div key={s.sectionId} style={{ marginBottom: 12 }}>
                            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4, display: s.title !== 'SKILLS' ? 'block' : 'none' }}>{s.title !== 'SKILLS' && s.title}</div>
                            <div className="preview-content">{s.content}</div>
                        </div>
                    ))}
                </div>
            )}

            {otherSections.map(s => s.visible && (
                <div key={s.sectionId}>
                    <div className="preview-section-title">{s.title}</div>
                    <div className="preview-content">{s.content}</div>
                </div>
            ))}
        </div>
    );
}
