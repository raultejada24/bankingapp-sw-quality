# Quality Control Practices for a Web Application

## Overview

This document outlines a comprehensive quality control initiative for a web application, encompassing static analysis, unit testing, refactoring, and continuous integration/deployment pipelines.

---

## Practice 2: Analysis and Unit Testing

### Static Code Analysis
- Configuration and integration with SonarCloud for automated code quality metrics
- Manual inspection of code structure and design patterns
- Identification of critical code smells including:
  - **Coupling and Encapsulation Issues**: High inter-class dependencies and violated access levels
  - **Primitive Obsession**: Overuse of primitive types instead of domain objects
  - **Data Clumps**: Repeated groupings of variables that should form cohesive types
  - **Magic Numbers**: Hardcoded numeric values lacking semantic meaning
  - **Feature Envy**: Methods accessing external class attributes excessively
  - **Missing Validation**: Lack of input and business logic validation

### Key Findings
- 22 distinct issues identified across the codebase
- Primary issues concentrated in `AccountService.java` and related domain classes
- Focus areas: data validation, architectural violations, and code maintainability

---

## Practice 3: Testing and Refactoring

### Testing Strategy
- **Unit Testing**: Comprehensive test suite using Mockito for mocking dependencies
- **Code Coverage**: Achieved 100% coverage with JaCoCo
- **End-to-End Testing**: Selenium WebDriver automation for critical user workflows
- **Test Structure**: Following Arrange-Act-Assert (AAA) pattern with descriptive test names

### Refactoring Efforts
Addressed identified code smells through targeted refactoring:
- Eliminated magic numbers by introducing semantic constants
- Replaced generic exceptions with custom, domain-specific exceptions
- Centralized validation logic to reduce duplication
- Improved method naming for better code readability
- Removed dead code and deprecated dependencies

### Testing Coverage
- Transfer functionality validation
- Deposit and withdrawal scenarios
- SMS and email notification workflows
- Boundary condition testing
- Error handling and exception scenarios

---

## Practice 4: CI/CD Pipelines and Collaborative Development

### Application Deployment
- **Platform**: Azure cloud infrastructure
- **Containerization**: Docker image versioning and registry management
- **Production Instance**: `banking-app-production` with automated updates

### GitHub Actions Workflows

#### Workflow 1: Per-Commit Validation
- **Trigger**: On push to non-main branches
- **Actions**: Unit test execution on `ubuntu-latest` runner
- **Purpose**: Isolate development code and validate test suite

#### Workflow 2: Pull Request Validation
- **Trigger**: When opening pull requests to main
- **Actions**: Execute both unit tests and end-to-end test suite
- **Purpose**: Ensure stability before integration

#### Workflow 3: Production Deployment
- **Trigger**: On merge to main branch
- **Jobs**:
  - **Build**: Construct Docker image, perform smoke test, publish to DockerHub
  - **Deploy**: Update Azure service and validate deployed version
- **Purpose**: Automated continuous deployment pipeline

#### Workflow 4: Scheduled Nightly Tests
- **Trigger**: Daily at 2:00 AM
- **Actions**: 
  - Cross-platform E2E testing (multiple OS and browsers)
  - Nightly build creation with timestamped versions
  - DockerHub image publication
- **Purpose**: Comprehensive regression testing and artifact distribution

### Semantic Versioning (SemVer)
- Base version: `1.0.0`
- Version updates reflected in application login screen
- MAJOR.MINOR.PATCH structure for all releases

### Collaborative Development with GitHubFlow

#### Feature 1: Daily Withdrawal Limit (€5,000 in 24 hours)
- **Implementation**: Stream-based transaction filtering for withdrawal calculation
- **Validation**: Custom exception handling
- **Version Bump**: 1.0.0 → 1.1.1

#### Feature 2: User Ban Status
- **Scope**: Added `banned` attribute to User entity
- **Enforcement**: Restrictions on deposit, withdrawal, and transfer operations
- **Version Bump**: 1.1.1 → 1.2.0

#### Feature 3: Age Restriction for Transfers
- **Implementation**: Birth date tracking and age-of-majority validation
- **Restriction**: Blocks transfers by users under 18 years old
- **Validation**: Dedicated age-check utility methods
- **Version Bump**: 1.2.0 → 1.3.0

#### Code Quality Refactoring
- Resolved 6 critical code smells
- Standardized semantic constants throughout codebase
- Version Bump: 1.0.0 → 1.0.1

### Development Workflow
