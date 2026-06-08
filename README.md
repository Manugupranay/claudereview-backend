# ClaudeReview Backend

A Spring Boot service that integrates with the Claude API to provide AI-powered code reviews.

## Overview

ClaudeReview Backend is a production-grade REST API that accepts code submissions and returns detailed, structured reviews powered by Anthropic's Claude model. It is the backend engine behind the ClaudeReview platform.

## Tech Stack

- **Java 17** + **Spring Boot 3.5**
- **Claude API** (Anthropic) for AI-powered analysis
- **Maven** for dependency management
- **Deployed on Railway**

## Features

- Submit code snippets for review via REST endpoint
- Receives structured feedback: bugs, improvements, best practices
- CORS configured for Netlify frontend
- Environment-based secrets management

## Getting Started

```bash
# Clone the repo
git clone https://github.com/Manugupranay/claudereview-backend.git
cd claudereview-backend

# Add your secrets
cp secrets.properties.example secrets.properties
# Fill in ANTHROPIC_API_KEY

# Run
./mvnw spring-boot:run
```

## API

See [API_CONTRACT.md](./API_CONTRACT.md) for full endpoint documentation.

## Live

Deployed at Railway. Frontend: [claudereview-frontend](https://github.com/Manugupranay/claudereview-frontend)

## License

MIT
