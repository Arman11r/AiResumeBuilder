-- Expand the 'type' ENUM column to include all notification types defined in the application
ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        -- AI events
        'ATS_COMPLETE',
        'AI_DONE',
        'AI_CONTENT_GENERATED',
        'COVER_LETTER_GENERATED',
        -- Resume events
        'RESUME_CREATED',
        'RESUME_DELETED',
        'RESUME_PUBLISHED',
        'RESUME_UNPUBLISHED',
        'SECTION_ADDED',
        'SECTION_DELETED',
        -- Export events
        'EXPORT_READY',
        -- Job events
        'JOB_MATCH',
        'JOB_SEARCH_COMPLETE',
        'JOB_BOOKMARKED',
        -- Account events
        'PLAN_UPGRADED',
        'PLAN_CHANGE',
        'QUOTA_WARNING',
        'PROFILE_UPDATED',
        'PASSWORD_CHANGED',
        -- Admin events
        'ADMIN_BROADCAST'
    ) NOT NULL;
