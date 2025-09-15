# Contributing to Netty-SocketIO

Thank you for your interest in contributing to Netty-SocketIO! This document provides guidelines and information for
contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Release Process](#release-process)
- [Community Guidelines](#community-guidelines)

## Code of Conduct

This project follows
the [Apache Software Foundation Code of Conduct](https://www.apache.org/foundation/policies/conduct.html). By
participating, you are expected to uphold this code.

## Getting Started

### Prerequisites

- **Java 11+** (required for building module-info)
- **Java 8+** (minimum runtime requirement)
- **Maven 3.0.5+**
- **Git**

## Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/netty-socketio.git
cd netty-socketio

# Add upstream remote
git remote add upstream https://github.com/mrniko/netty-socketio.git
```

### 2. Build the Project

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Run integration tests
mvn test -Dtest=*IntegrationTest

# Build with all checks
mvn clean verify
```

### 3. IDE Setup

The project uses standard Maven structure. Import as a Maven project in your IDE.

**Recommended IDE settings:**

- Use UTF-8 encoding
- Set line endings to LF (Unix)
- Enable auto-formatting on save
- Configure Checkstyle plugin if available

## Coding Standards

### Code Style

The project uses Checkstyle for code quality enforcement. Configuration is in `checkstyle.xml`.

**Key style guidelines:**

- Follow Java naming conventions
- Use 4 spaces for indentation (no tabs)
- Maximum method length: reasonable (no hard limit)
- Maximum parameters: 10
- Maximum nested depth: 2 for loops, 3 for if statements
- No trailing whitespace
- No unused imports
- Use meaningful variable and method names

### Code Quality Tools

The project enforces several quality checks:

- **Checkstyle**: Code style enforcement
- **PMD**: Static code analysis
- **Maven Enforcer**: Dependency and version checks
- **License Plugin**: Header validation

Run quality checks:

```bash
mvn checkstyle:check
mvn pmd:check
mvn license:check
```

### License Headers

All source files must include the Apache 2.0 license header. The header template is in `header.txt`.

```java
/*
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Testing Guidelines

### Test Structure

The project has comprehensive test coverage:

- **Unit Tests**: Located in `src/test/java`
- **Integration Tests**: Located in `src/test/java/com/corundumstudio/socketio/integration/`, based on TestContainers
- **Parser Tests**: Protocol parsing tests
- **Transport Tests**: WebSocket and HTTP transport tests

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BasicConnectionTest

# Run integration tests
mvn test -Dtest=*IntegrationTest

# Run with specific Java version
mvn test -Djava.version=11
```

### Writing Tests

**Test Requirements:**

- Use JUnit 4 (current version)
- Use JMockit for mocking
- Follow AAA pattern (Arrange, Act, Assert)
- Use descriptive test method names
- Include both positive and negative test cases
- Test edge cases and error conditions

**Example test structure:**

```java

@Test
public void testMethodName_WhenCondition_ShouldExpectedResult() {
    // Arrange
    // Setup test data and mocks

    // Act
    // Execute the method under test

    // Assert
    // Verify the results
}
```

### Integration Testing

Integration tests use TestContainers for Redis testing and Socket.IO clients for end-to-end validation.

**Key integration test scenarios:**

- Basic client connection/disconnection
- Event handling and broadcasting
- Room management
- Namespace support
- Acknowledgment callbacks
- Concurrent connections
- Error handling

## Pull Request Process

### Before Submitting

1. **Check existing issues**: Search for related issues or discussions
2. **Create an issue**: For significant changes, create an issue first
3. **Fork and branch**: Create a feature branch from `master`
4. **Follow coding standards**: Ensure code passes all quality checks
5. **Write tests**: Add tests for new functionality
6. **Update documentation**: Update relevant documentation

### Branch Naming

Use descriptive branch names:

- `feature/description` - New features
- `fix/description` - Bug fixes
- `refactor/description` - Code refactoring
- `test/description` - Test improvements

### Commit Messages

Follow conventional commit format:

```
type(scope): description

[optional body]

[optional footer]
```

**Types:**

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Build/tooling changes

**Examples:**

```
feat(transport): add WebSocket compression support
fix(parser): handle malformed JSON packets gracefully
docs(readme): update installation instructions
```

### Pull Request Checklist

- [ ] Code follows project coding standards
- [ ] All tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventional format
- [ ] Branch is up to date with master
- [ ] No merge conflicts
- [ ] DCO (Developer Certificate of Origin) signed

### Review Process

1. **Automated checks**: All CI checks must pass
2. **Code review**: At least one maintainer review required
3. **Testing**: Manual testing may be requested
4. **Documentation**: Ensure documentation is updated
5. **Approval**: Maintainer approval required for merge

## Developer Certificate of Origin (DCO)

This project uses the Developer Certificate of Origin (DCO) to ensure that contributors have the right to submit their
contributions.

### How to sign your commits

To certify your contributions, you need to add a `Signed-off-by` line to your commit messages:

```
git commit -s -m "Your commit message"
```

This will add a line like:

```
Signed-off-by: Your Name <your.email@example.com>
```

### What the DCO means

By signing off your commits, you certify that you wrote the patch or have the right to pass it on as an open-source
patch.

## Release Process

### Versioning

The project follows [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking API changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Workflow

1. **Version bump**: Update version in `pom.xml`
2. **Changelog**: Update `README.md` with release notes
3. **Tag**: Create git tag for the release
4. **Build**: Run release profile build
5. **Deploy**: Deploy to Maven Central

### Release Profile

The project uses Maven release profile for publishing:

- Source JAR generation
- Javadoc JAR generation
- GPG signing
- Checksum generation
- Nexus staging

## Community Guidelines

### Getting Help

- **GitHub Issues**: For bug reports and feature requests
- **Discussions**: For questions and general discussion
- **Documentation**: Check README and code comments first

### Reporting Issues

**Bug Reports should include:**

- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (Java version, OS, etc.)
- Minimal code example if applicable
- Logs or stack traces

**Feature Requests should include:**

- Clear description of the feature
- Use case and motivation
- Proposed implementation approach (if any)
- Backward compatibility considerations

### Contributing Guidelines

- **Be respectful**: Treat all community members with respect
- **Be constructive**: Provide helpful feedback and suggestions
- **Be patient**: Maintainers are volunteers, responses may take time
- **Be thorough**: Provide complete information in issues and PRs
- **Be collaborative**: Work together to improve the project

### Recognition

Contributors are recognized in:

- Release notes
- GitHub contributors list
- Project documentation (where appropriate)

## Development Workflow

### Daily Development

1. **Sync with upstream**: `git fetch upstream && git rebase upstream/master`
2. **Create feature branch**: `git checkout -b feature/your-feature`
3. **Make changes**: Follow coding standards
4. **Test locally**: `mvn clean verify`
5. **Commit changes**: Use conventional commit format
6. **Push branch**: `git push origin feature/your-feature`
7. **Create PR**: Submit pull request

### Continuous Integration

The project uses GitHub Actions for CI:

- **Build PR**: Tests on Java 17 and 21
- **DCO Check**: Verifies commit signatures
- **Quality Gates**: Checkstyle, PMD, and other checks

### Performance Considerations

When contributing performance-related changes:

- **Benchmark**: Include performance benchmarks
- **Memory**: Consider memory usage impact
- **Scalability**: Test with multiple clients
- **Documentation**: Document performance characteristics

## Additional Resources

- **Socket.IO Protocol**: [Official documentation](https://socket.io/docs/v4/)
- **Netty Documentation**: [Netty user guide](https://netty.io/wiki/)
- **Maven Guide**: [Maven getting started](https://maven.apache.org/guides/getting-started/)
- **Java Module System**: [JPMS guide](https://openjdk.java.net/projects/jigsaw/quick-start)

## Questions?

If you have questions about contributing, please:

1. Check this document first
2. Search existing issues and discussions
3. Create a new issue with the "question" label
4. Join community discussions

Thank you for contributing to Netty-SocketIO! 🚀
