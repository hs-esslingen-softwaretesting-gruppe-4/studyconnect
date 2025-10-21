## StudyConnect Requirements

### Scope and Context
This document captures the initial functional and non-functional requirements for the StudyConnect application, derived from the introductory description in `exercise 2 PDF`. Requirements use “shall” wording and are grouped for clarity. Items marked as [VAGUE] indicate areas that need further clarification and agreement with stakeholders.

### Legend
- ID format: FR-n for Functional, NFR-n for Non-Functional
- Priority: Must, Should, Could

---

## Functional Requirements

### 1. Task Management (Individual)
- FR-1 (Must): The system shall allow users to create personal tasks with title, due date, priority, and notes. (Source: Intro)
- FR-2 (Must): The system shall allow users to edit and delete their personal tasks. (Source: Intro)
- FR-3 (Must): The system shall allow users to set and update a task status among at least: open, in progress, completed. (Source: Intro)
- FR-4 (Should): The system shall allow users to categorize tasks into thematic categories (e.g., “Mathematics,” “Exam Prep,” “Group Project”). (Source: Intro)
- FR-5 (Could): The system shall support filtering and sorting tasks by category, due date, priority, and status. (Source: Intro)
- FR-6 (Could): The system shall support bulk status updates for selected tasks. (Source: Derived)

[VAGUE] Clarify whether tasks support subtasks/checklists, attachments, and custom fields.

### 2. Group Collaboration
- FR-7 (Must): The system shall allow users to create study groups. (Source: Intro)
- FR-8 (Must): The system shall allow users to join existing study groups by invitation or request. (Source: Intro)
- FR-9 (Must): The system shall allow group administrators to invite new members and remove existing members. (Source: Intro)
- FR-10 (Should): The system shall allow assigning tasks to one or more group members. (Source: Intro)
- FR-11 (Should): The system shall provide a group task board or list view showing shared tasks and their statuses. (Source: Intro)

[VAGUE] Clarify group discovery (private/public), maximum group size, and approval flow for join requests.

### 3. Roles and Permissions
- FR-12 (Must): The system shall distinguish at least two roles within a group: administrator and member. (Source: Intro)
- FR-13 (Must): The system shall restrict administrative actions (invite/remove members, assign roles) to group administrators. (Source: Intro)
- FR-14 (Should): The system shall allow administrators to reassign the administrator role to another member. (Source: Derived)

[VAGUE] Clarify whether additional roles (e.g., moderator) or per-permission granularity are required.

### 4. Deadline Awareness and Progress
- FR-15 (Must): The system shall display task due dates and highlight overdue tasks. (Source: Intro)
- FR-16 (Should): The system shall present upcoming tasks and milestones in a calendar or agenda view. (Source: Intro)
- FR-17 (Should): The system shall provide reminders/notifications for upcoming and overdue tasks. (Source: Intro)

[VAGUE] Define notification channels (in-app, email, push), schedules, quiet hours, and user preferences.

### 5. Communication Around Tasks
- FR-18 (Should): The system shall provide commenting on tasks to keep discussion tied to the relevant item. (Source: Intro)
- FR-19 (Could): The system shall provide lightweight messaging within a group context to discuss shared responsibilities. (Source: Intro)

[VAGUE] Clarify if real-time chat is required vs. asynchronous comments, and whether threading, mentions, and reactions are needed.

### 6. Motivation and Gamification
- FR-20 (Could): The system shall award progress points or badges for completed tasks. (Source: Intro)
- FR-21 (Could): The system shall show a user’s cumulative progress metrics within a chosen time window. (Source: Derived)

[VAGUE] Define specific gamification rules, badge taxonomy, anti-gaming measures, and visibility to others.

### 7. Data Export and Integration
- FR-22 (Should): The system shall allow export of tasks/schedules to PDF. (Source: Intro)
- FR-23 (Should): The system shall allow export of calendar data in ICS format. (Source: Intro)

[VAGUE] Clarify export scope (personal vs. group), filters, localization, and inclusion of comments/attachments.

### 8. Cross-Platform Availability
- FR-24 (Should): The system shall be accessible via a web interface usable on desktop and mobile browsers. (Source: Intro)
- FR-25 (Could): The system shall provide mobile apps or responsive PWA capabilities for offline-first access. (Source: Intro)

[VAGUE] Clarify target platforms (iOS/Android/Web/PWA/Desktop), offline requirements, and native capabilities (push, background sync).

### 9. Accessibility and Inclusivity
- FR-26 (Must): The system shall provide accessible interaction flows for task and group management consistent with the defined accessibility baseline. (Source: Intro)

[VAGUE] Specify concrete accessibility targets and assistive technology support.

---

## Non-Functional Requirements (ISO/IEC 25010 aligned)

### 1. Usability and Accessibility
- NFR-1 (Must): The UI shall be intuitive such that new users can create a task within 2 minutes without training, measured via usability testing with at least 10 users. (Source: Intro)
- NFR-2 (Must): The product shall conform to WCAG 2.1 AA for the web interface. (Source: Intro)
- NFR-3 (Should): Core flows (create/edit/assign task) shall require no more than 5 clear steps end-to-end. (Source: Intro)

[VAGUE] Confirm WCAG level and specific assistive technology testing scope.

### 2. Reliability and Availability
- NFR-4 (Should): The service shall achieve 99.5% monthly uptime excluding planned maintenance windows. (Source: Derived)
- NFR-5 (Should): The system shall prevent data loss by storing changes atomically and by daily backups retained for 30 days. (Source: Derived)

[VAGUE] Confirm RPO/RTO targets and backup encryption/geo-redundancy.

### 3. Performance and Scalability
- NFR-6 (Should): Task list views shall load initial content within 2.0 seconds at p50 and 4.0 seconds at p95 for typical user datasets (≤1,000 tasks). (Source: Derived)
- NFR-7 (Should): Adding or updating a task shall complete within 1.0 second p95. (Source: Derived)
- NFR-8 (Could): The system shall scale to 10,000 active users with linear resource growth. (Source: Derived)

[VAGUE] Validate dataset sizes, concurrency expectations, and realistic target user counts for the course context.

### 4. Security and Privacy
- NFR-9 (Must): All data in transit shall be protected with TLS 1.2+; all credentials shall be hashed using a modern algorithm (e.g., Argon2id). (Source: Derived)
- NFR-10 (Must): The system shall implement role-based access control enforcing group-level permissions. (Source: Intro)
- NFR-11 (Must): The system shall comply with GDPR principles for data minimization, purpose limitation, and user data rights (export/delete). (Source: Derived)
- NFR-12 (Should): PII and sensitive data at rest shall be encrypted using industry-standard mechanisms. (Source: Derived)

[VAGUE] Confirm exact PII categories, data retention policy, DSR SLAs, and lawful bases for processing.

### 5. Maintainability and Testability
- NFR-13 (Must): The codebase shall include automated unit tests covering at least 60% of core domain logic by the end of iteration 2. (Source: Derived)
- NFR-14 (Should): The system shall include API contract tests for critical endpoints (tasks, groups, membership) executed in CI on each commit. (Source: Derived)
- NFR-15 (Should): The system shall include an automated linting and formatting pipeline enforced in CI. (Source: Derived)
- NFR-16 (Should): The architecture shall separate concerns (e.g., presentation, application, domain, persistence) to facilitate change. (Source: Derived)

### 6. Portability and Compatibility
- NFR-17 (Should): The web app shall support the latest versions of Chrome, Firefox, Safari, and Edge. (Source: Derived)
- NFR-18 (Could): The mobile experience shall be responsive for small screens down to 360×640 logical pixels. (Source: Derived)




