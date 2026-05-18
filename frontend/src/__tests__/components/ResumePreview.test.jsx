/**
 *   - renders nothing when resume prop is null/undefined
 *   - renders the resume title as an <h1>
 *   - renders the target job title
 *   - renders SUMMARY section content when visible
 *   - does NOT render SUMMARY section when visible=false
 *   - renders EXPERIENCE section headings
 *   - renders EDUCATION section content
 *   - renders SKILLS section content
 *   - renders custom "other" sections with their titles
 *   - skips sections that are not visible
 *   - falls back to "Your Name" when title is missing
 *   - falls back to "Target Role" when targetJobTitle is missing
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ResumePreview from '../../components/ResumePreview';

// Shared fixtures

const baseResume = {
  resumeId: 'resume-001',
  title: 'John Doe',
  targetJobTitle: 'Senior Software Engineer',
};

const summarySection = {
  sectionId: 's-001',
  sectionType: 'SUMMARY',
  title: 'Professional Summary',
  content: 'Experienced software engineer with 8+ years.',
  displayOrder: 1,
  visible: true,
};

const expSection = {
  sectionId: 's-002',
  sectionType: 'EXPERIENCE',
  title: 'Software Engineer at ACME',
  content: 'Built scalable microservices.',
  displayOrder: 2,
  visible: true,
};

const eduSection = {
  sectionId: 's-003',
  sectionType: 'EDUCATION',
  title: 'B.Tech Computer Science',
  content: 'IIT Bombay, 2016',
  displayOrder: 3,
  visible: true,
};

const skillsSection = {
  sectionId: 's-004',
  sectionType: 'SKILLS',
  title: 'SKILLS',
  content: 'Java, Spring Boot, Docker, Kubernetes',
  displayOrder: 4,
  visible: true,
};

const customSection = {
  sectionId: 's-005',
  sectionType: 'CUSTOM',
  title: 'Volunteer Work',
  content: 'Taught coding to underprivileged kids.',
  displayOrder: 5,
  visible: true,
};

// Null / undefined resume

describe('ResumePreview – null resume', () => {
  test('renders nothing when resume prop is null', () => {
    // Arrange & Act
    const { container } = render(<ResumePreview resume={null} sections={[]} />);

    // Assert
    expect(container).toBeEmptyDOMElement();
  });

  test('renders nothing when resume prop is undefined', () => {
    // Arrange & Act
    const { container } = render(<ResumePreview resume={undefined} sections={[]} />);

    // Assert
    expect(container).toBeEmptyDOMElement();
  });
});

// Header: name and job title

describe('ResumePreview – header rendering', () => {
  test('renders resume title inside an h1 element', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[]} />);

    // Assert
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('John Doe');
  });

  test('renders the target job title', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[]} />);

    // Assert
    expect(screen.getByText('Senior Software Engineer')).toBeInTheDocument();
  });

  test('falls back to "Your Name" when title is empty', () => {
    // Arrange
    const resumeNoTitle = { ...baseResume, title: '' };

    // Act
    render(<ResumePreview resume={resumeNoTitle} sections={[]} />);

    // Assert
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Your Name');
  });

  test('falls back to "Target Role" when targetJobTitle is missing', () => {
    // Arrange
    const resumeNoRole = { ...baseResume, targetJobTitle: '' };

    // Act
    render(<ResumePreview resume={resumeNoRole} sections={[]} />);

    // Assert
    expect(screen.getByText('Target Role')).toBeInTheDocument();
  });
});

// SUMMARY section

describe('ResumePreview – SUMMARY section', () => {
  test('renders summary content when section is visible', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[summarySection]} />);

    // Assert
    expect(screen.getByText('Experienced software engineer with 8+ years.')).toBeInTheDocument();
  });

  test('does NOT render summary content when section is not visible', () => {
    // Arrange
    const hiddenSummary = { ...summarySection, visible: false };

    // Act
    render(<ResumePreview resume={baseResume} sections={[hiddenSummary]} />);

    // Assert
    expect(screen.queryByText('Experienced software engineer with 8+ years.')).not.toBeInTheDocument();
  });
});


// EXPERIENCE section


describe('ResumePreview – EXPERIENCE section', () => {
  test('renders the "Experience" heading when experience sections exist', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[expSection]} />);

    // Assert
    expect(screen.getByText('Experience')).toBeInTheDocument();
  });

  test('renders the experience entry title', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[expSection]} />);

    // Assert
    expect(screen.getByText('Software Engineer at ACME')).toBeInTheDocument();
  });

  test('renders the experience content', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[expSection]} />);

    // Assert
    expect(screen.getByText('Built scalable microservices.')).toBeInTheDocument();
  });

  test('does NOT show the "Experience" heading when list is empty', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[]} />);

    // Assert
    expect(screen.queryByText('Experience')).not.toBeInTheDocument();
  });
});


// EDUCATION section


describe('ResumePreview – EDUCATION section', () => {
  test('renders the "Education" heading when education sections exist', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[eduSection]} />);

    // Assert
    expect(screen.getByText('Education')).toBeInTheDocument();
  });

  test('renders the education entry content', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[eduSection]} />);

    // Assert
    expect(screen.getByText('IIT Bombay, 2016')).toBeInTheDocument();
  });
});


// SKILLS section


describe('ResumePreview – SKILLS section', () => {
  test('renders the "Skills" heading when skills sections exist', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[skillsSection]} />);

    // Assert
    expect(screen.getByText('Skills')).toBeInTheDocument();
  });

  test('renders the skills content', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[skillsSection]} />);

    // Assert
    expect(screen.getByText('Java, Spring Boot, Docker, Kubernetes')).toBeInTheDocument();
  });
});


// Custom "other" sections


describe('ResumePreview – custom sections', () => {
  test('renders custom section title and content', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={[customSection]} />);

    // Assert
    expect(screen.getByText('Volunteer Work')).toBeInTheDocument();
    expect(screen.getByText('Taught coding to underprivileged kids.')).toBeInTheDocument();
  });
});


// Multiple sections together


describe('ResumePreview – full resume render', () => {
  const allSections = [summarySection, expSection, eduSection, skillsSection, customSection];

  test('renders all section types correctly', () => {
    // Arrange & Act
    render(<ResumePreview resume={baseResume} sections={allSections} />);

    // Assert
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('John Doe');
    expect(screen.getByText(/Experienced software engineer/)).toBeInTheDocument();
    expect(screen.getByText('Experience')).toBeInTheDocument();
    expect(screen.getByText('Education')).toBeInTheDocument();
    expect(screen.getByText('Skills')).toBeInTheDocument();
    expect(screen.getByText('Volunteer Work')).toBeInTheDocument();
  });

  test('hidden sections are not rendered in the final output', () => {
    // Arrange
    const hiddenExp = { ...expSection, visible: false };
    const sections = [summarySection, hiddenExp, eduSection];

    // Act
    render(<ResumePreview resume={baseResume} sections={sections} />);

    // Assert – experience content should be absent
    expect(screen.queryByText('Built scalable microservices.')).not.toBeInTheDocument();
  });
});
