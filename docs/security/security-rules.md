# Security Rules 

## Purpose

Define enforceable security rules for human, automated, and AI review.

Applies to Authentication, Authorization, Access Control, Input Validation, Data Protection, Secrets, APIs, Injection, External Calls, Resources, Logging, Cryptography, Dependencies, Configuration, and security-sensitive Transactions/Concurrency.

## Core Rules

| ID    | Rule                                                                                                                   |
| ----- | ---------------------------------------------------------------------------------------------------------------------- |
| S-001 | Protected resources require Authentication.                                                                            |
| S-002 | Enforce Authorization at the appropriate boundary; Authentication alone is insufficient.                               |
| S-003 | Apply Least Privilege to users, services, roles, credentials, and resources.                                           |
| S-004 | Never hard-code secrets, credentials, tokens, or private keys.                                                         |
| S-005 | Never expose secrets or sensitive data through logs, errors, responses, or debug output.                               |
| S-006 | Validate and constrain all untrusted input at system boundaries.                                                       |
| S-007 | Prevent Injection; use parameterized queries and safe APIs.                                                            |
| S-008 | Never trust client-provided identity, roles, permissions, or security-sensitive fields without server-side validation. |
| S-009 | Protect sensitive data in transit and at rest according to its requirements.                                           |
| S-010 | Use approved cryptography and secure key management; never implement custom cryptography.                              |
| S-011 | Avoid insecure deserialization of untrusted or insufficiently trusted data.                                            |
| S-012 | Secure external service calls with appropriate authentication, authorization, validation, and failure handling.        |
| S-013 | Restrict file, path, resource, and network access to explicitly allowed targets.                                       |
| S-014 | Return safe errors; never expose stack traces, internals, credentials, or sensitive implementation details.            |
| S-015 | Apply security controls consistently across equivalent access paths; prevent bypasses.                                 |
| S-016 | Do not weaken or bypass security controls without an approved security/architectural decision.                         |
| S-017 | Dependencies and configuration must not introduce known or avoidable security risks.                                   |
| S-018 | Security-sensitive operations must preserve required auditability and traceability.                                    |

## Security Requirements

Project-specific or numeric requirements must be defined separately.

Examples: Authentication, Token Lifetime, Rate Limits, Secret Policy, Encryption, Audit, Data Retention, Authentication Attempts.

**Security Agent must not invent Security Requirements.**

## Security Review

Security Agent must:

1. Analyze the diff and relevant Security Context.
2. Check Security Requirements, Rules, ADRs, and relevant Business Rules.
3. Detect security vulnerabilities and control bypasses.
4. Provide Evidence from the diff.
5. Classify the finding as `CONFIRMED`, `POTENTIAL`, or `SUGGESTION`.
6. Assign Severity and Confidence.
7. Explain Risk and suggest remediation when possible.

## Finding Classification

* `CONFIRMED`: Rule/Requirement violation established by available evidence.
* `POTENTIAL`: Security-risk pattern exists, but required runtime/configuration evidence is unavailable.
* `SUGGESTION`: Optional security hardening without an established violation.

## Severity

* `BLOCKER`: Critical vulnerability or severe explicit security violation.
* `HIGH`: Significant risk of unauthorized access, data exposure, injection, credential compromise, or security-control bypass.
* `MEDIUM`: Security weakness with significant risk under certain conditions.
* `LOW`: Minor security weakness or hardening opportunity.

## Constraints

Security remediation must not violate:

* Business Rules
* Domain Invariants
* Architecture Rules
* Data Consistency
* Transaction Semantics
* Availability Requirements

Security objectives:

`Confidentiality + Integrity + Availability + Least Privilege + Auditability`

## Source of Truth

1. Explicit Security Requirements
2. Approved Security/Architecture ADRs
3. Project-specific Security Rules
4. General Security Rules
5. AI Suggestions

AI-generated Suggestions alone must not trigger `REQUEST_CHANGES`.

## Rule Convention

Security Rules use unique IDs: `S-XXX`.

Current rules: `S-001`–`S-018`.
