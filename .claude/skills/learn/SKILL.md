---
name: learn
description: Provides autonomous project pattern learning by analyzing the codebase to discover development conventions, architectural patterns, and coding standards, then generates project rule files in .claude/rules/. Use when user asks to "learn from project", "extract project rules", "analyze codebase conventions", "discover project patterns", or wants to auto-generate Claude Code rules for the current project.
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, AskUserQuestion
---

# Learn

Autonomously analyzes a project's codebase to discover development patterns, conventions, and architectural decisions, then generates project rule files in `.claude/rules/` for Claude Code to follow.

## Overview

This skill runs a single self-contained analysis pass: gather project context, do a forensic scan of the codebase for real conventions, filter and rank the findings, present them to the user, and persist only the approved rules to `.claude/rules/`. It does not depend on any other agent or skill.

## When to Use

Use this skill when:

- User asks to "learn from this project" or "understand project conventions"
- User wants to auto-generate `.claude/rules/` files from the existing codebase
- User asks to "extract project rules" or "discover patterns"
- User wants Claude Code to learn the project's coding standards
- After joining a new project and wanting to codify existing conventions
- Before starting a large feature to ensure Claude follows project patterns

**Trigger phrases:** "learn from project", "extract rules", "analyze conventions", "discover patterns", "generate project rules", "learn codebase", "auto-generate rules"

## Instructions

### Phase 1: Project Context Assessment

Gather high-level project context before the deep scan:

1. **Verify project root**: Confirm the current working directory is a project root (has `package.json`, `pom.xml`, `pyproject.toml`, `go.mod`, `.git/`, or similar markers)

2. **Check existing rules**: Scan for pre-existing rule files to understand what is already documented:

```bash
# Check for existing rules
ls -la .claude/rules/ 2>/dev/null || echo "No .claude/rules/ directory found"
cat CLAUDE.md 2>/dev/null || echo "No CLAUDE.md found"
cat AGENTS.md 2>/dev/null || echo "No AGENTS.md found"
ls -la .cursorrules 2>/dev/null || echo "No .cursorrules found"
```

3. **Assess project size**: Get a quick overview of the project scope:

```bash
# Quick project overview
find . -maxdepth 1 -type f -name "*.json" -o -name "*.toml" -o -name "*.xml" -o -name "*.gradle*" -o -name "Makefile" -o -name "*.yaml" -o -name "*.yml" | head -20
find . -type f -name "*.ts" -o -name "*.js" -o -name "*.java" -o -name "*.py" -o -name "*.go" -o -name "*.php" | wc -l
```

4. **Inform the user**: Briefly tell the user what you found and that you are about to start analysis:
   - "I found a [TypeScript/NestJS] project with [N] source files and [M] existing rules. Starting deep analysis..."

### Phase 2: Forensic Codebase Analysis

Do this scan yourself, directly — do not delegate it to another agent.

1. **Sample representative files** across the project rather than reading everything: pick 2-4 files per major directory/module (controllers, services, tests, config) so patterns are evidenced across multiple locations, not guessed from one file.

2. **Look for recurring, non-obvious conventions** in areas such as:
   - Directory/module organization (e.g., every feature folder has the same subfolder shape)
   - Naming conventions (file, class, function, test naming patterns)
   - Error/response envelope shapes used consistently across endpoints
   - Validation, DTO, or schema patterns repeated across models
   - Test structure/fixture conventions
   - State management or data-flow patterns repeated across components
   - Any convention that a new contributor would NOT guess correctly on the first try

3. **For each candidate pattern found, require**:
   - A clear, specific title (not "Naming Convention" — "Repository Method Naming: findBy/existsBy/countBy prefix")
   - Evidence from **at least 2 files** — a pattern seen once is a coincidence, not a convention
   - An impact score (1-10): how much would violating this pattern confuse Claude Code or break the project's consistency?
   - Discard anything with impact < 4 — trivial or purely stylistic preferences aren't worth a rule file

4. **Deduplicate against existing rules**: compare each candidate's title and content against files already found in `.claude/rules/`, `CLAUDE.md`, or `AGENTS.md` in Phase 1. Skip anything that duplicates existing documentation.

5. **Select the top 3** remaining findings by impact score. If fewer than 3 qualify, present whatever legitimately qualifies — never pad the list with low-confidence findings just to reach 3.

6. **If zero findings remain**: tell the user the project is already well-documented or no significant undocumented pattern was found. This is a valid, expected outcome — do not force output.

### Phase 3: Present to User

Present the findings in a clear, structured format:

```
I analyzed your codebase and found N patterns worth documenting as project rules:

1. **[RULE]** <Title> (Impact: X/10)
   <One-line explanation, with the file evidence it's based on>

2. **[RULE]** <Title> (Impact: X/10)
   <One-line explanation, with the file evidence it's based on>

3. **[RULE]** <Title> (Impact: X/10)
   <One-line explanation, with the file evidence it's based on>
```

Then ask the user for confirmation using **AskUserQuestion**:

- Present choices: "Save all N rules", "Let me choose which ones to save", "Cancel — don't save anything"
- If the user wants to select individually, present each rule one by one with "Save / Skip" options
- **Never save automatically** — always require explicit user approval

### Phase 4: Persist Approved Rules

For each approved rule:

1. **Ensure directory exists**:

```bash
mkdir -p .claude/rules
```

2. **Generate the file name**: Use the finding's title converted to kebab-case:
   - Example: `"API Response Envelope Convention"` → `api-response-envelope-convention.md`
   - Avoid generic names like `rule-1.md` or `learned-pattern.md`

3. **Check for conflicts**: Before writing, check if a file with the same name already exists:
   - If it exists, present a diff to the user and ask whether to replace, merge, or skip

4. **Write the rule file**: Create the file in `.claude/rules/` with well-formed markdown content: the rule statement, the evidence (file paths), and why it matters.

5. **Confirm to user**: After saving, list all created files:

```
✅ Rules saved successfully:

  .claude/rules/api-response-envelope-convention.md
  .claude/rules/feature-based-module-organization.md
  .claude/rules/test-factory-pattern.md

These rules will be automatically applied by Claude Code in future sessions.
```

## Best Practices

1. **Run early in a project**: Use this skill when joining a new project to quickly codify conventions
2. **Review before saving**: Always verify the generated rules make sense for your project
3. **Iterate**: Run the skill periodically as the project evolves — new patterns may emerge
4. **Edit after saving**: Generated rules are starting points; refine them to match your exact preferences
5. **Commit rules to git**: `.claude/rules/` files are project-specific and should be version-controlled so the whole team benefits

## Constraints and Warnings

### Critical Constraints

1. **Never save without confirmation**: Always ask the user before writing any files
2. **Project-local only**: Only write to `.claude/rules/` in the current project directory, never to global paths
3. **Read-only analysis**: The analysis phase must not modify any project files
4. **Evidence-based**: Every rule must be backed by concrete evidence from at least 2 files in the codebase
5. **No hallucination**: Do not invent patterns that are not actually present in the codebase
6. **Respect existing rules**: Do not overwrite existing rules without explicit user approval
7. **Keep rules focused**: Each rule file should address one specific convention or pattern

### Limitations

- **Large monorepos**: Analysis may take longer on very large codebases. Scan representative samples, not every file.
- **Polyglot projects**: In multi-language projects, rules are generated per-language. Ensure the rule title indicates the language scope.
- **Existing rules conflict**: If the project already has comprehensive `.claude/rules/`, the skill may find few or no new patterns. This is expected.
- **Dynamic patterns**: Some patterns only emerge at runtime (e.g., middleware ordering). This skill focuses on static codebase analysis.

## Examples

### Example 1: Learning from a NestJS project

**User request:** "Learn from this project"

**Phase 1 — Context assessment:**
```
Found: TypeScript/NestJS project with 142 source files
Existing rules: 0 files in .claude/rules/
Starting deep analysis...
```

**Phase 3 — Presentation:**
```
I analyzed your codebase and found 3 patterns worth documenting as project rules:

1. **[RULE]** Feature-Based Module Organization (Impact: 9/10)
   All modules follow src/modules/<feature>/ with controller, service, dto, entity subdirectories.

2. **[RULE]** DTO Validation Convention (Impact: 8/10)
   All DTOs use class-validator decorators and follow Create/Update naming pattern.

3. **[RULE]** Error Response Envelope (Impact: 7/10)
   All API errors return { statusCode, message, error } consistent envelope format.

Save all 3 rules? [Save all / Let me choose / Cancel]
```

**Phase 4 — Persistence:**
```
✅ Rules saved successfully:

  .claude/rules/feature-based-module-organization.md
  .claude/rules/dto-validation-convention.md
  .claude/rules/error-response-envelope.md

These rules will be automatically applied by Claude Code in future sessions.
```

### Example 2: Project with existing rules

**User request:** "Discover project patterns"

**Phase 1 — Context assessment:**
```
Found: Java/Spring Boot project with 87 source files
Existing rules: 4 files in .claude/rules/
Starting deep analysis...
```

**Phase 2 — After filtering:**
```
Found 6 candidate patterns, but 4 overlap with your existing rules.
After deduplication, 2 new patterns remain:

1. **[RULE]** Repository Method Naming (Impact: 7/10)
   All custom repository methods use findBy/existsBy/countBy prefix convention.

2. **[RULE]** Integration Test Database Strategy (Impact: 6/10)
   Integration tests use @Testcontainers with PostgreSQL and @Sql for fixtures.

Save these 2 rules? [Save all / Let me choose / Cancel]
```
