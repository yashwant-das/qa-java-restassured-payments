# Payments API Repository Standards Adoption Plan

> **For agentic workers:** Use `superpowers:executing-plans` to implement this plan task by task. Track the checkboxes below. This document is a planning record, not current setup guidance.

**Goal:** Apply the complete repo-standards profile while preserving the payment framework's engineering depth, runnable examples and report evidence.

**Architecture:** Retain the Maven reactor's separate `backend/` system under test and `automation/` client/test modules. Consolidate documentation by audience, align Java namespaces, review the dependency compatibility set, and add consistent local and CI gates without changing payment behavior.

**Tech stack:** Java 21, Maven, Spring Boot, REST Assured, TestNG, MySQL, Allure, Docker Compose and GitHub Actions.

**Spec:** The user's ten preservation and verification requirements, plus [repo-standards at a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9](https://github.com/yashwant-das/repo-standards/tree/a429dd2f2da55a4c3e1c73c7c0d1111f9cd0b9a9). Read its README and all five policy guides before execution.

## Global constraints

- Profile: `complete`. Record the pinned standards SHA in `AGENTS.md`; keep detailed evidence and exceptions in `docs/development.md`.
- Preserve Java 21. Verify compatible library updates against official registries and release notes; do not silently change the runtime baseline.
- Preserve the separate modules, all eight API endpoints, existing assertions, signing/replay rejection, four-thread execution and report attachments.
- Keep the README focused on purpose, capabilities and how to use and verify the project. Explain implementation details in the architecture guide.
- Preserve useful content before deleting or moving its source; update incoming links in the same change.
- Never describe a configured check as an executed or branch-protection-enforced check. Record unexecuted checks and exceptions with reason, owner and review trigger.
- Make no changes to sibling repositories. Do not bypass failed hooks, weaken checks or broaden workflow permissions.

## Baseline findings

| Item | Observed state on 2026-10-28 |
| --- | --- |
| Target | `qa-java-restassured-payments`, clean `main` at `31ac98adb4bdbb8224cbf0fc37602a7592a52883` before this plan |
| Pull | `git pull --ff-only` succeeded: `Already up to date.` |
| Purpose | End-to-end testing of a bundled signed, stateful payment API, including direct MySQL assertions |
| Showcase strengths | Atomic monotonic signing timestamps; full subscription lifecycle; transaction/receipt-scoped SQL and audit checks; negative security/business cases; Allure HTTP and SQL evidence |
| Existing docs | README has source-linked capabilities, Mermaid, quickstart, coverage matrix, CI table, stack and Allure screenshot; `docs/framework-guide.md` contains valuable design, API, configuration and extension guidance |
| Test inventory | 14 annotated test methods across four concrete classes; TestNG uses `parallel="methods"` and `thread-count="4"`. This is a source inventory, not an execution result |
| Structure gap | Existing `io.github.yashwantdas` Maven group and Java base namespace differ from the standard |
| Documentation gaps | Missing `AGENTS.md`, `docs/architecture.md`, `docs/development.md` and Markdown lint configuration |
| Gate gaps | No pre-commit configuration; workflows use action tags, omit checkout credential persistence settings and explicit artifact retention; Docker Dependabot updates are monthly |
| Local prerequisites | Maven and Docker CLI present; Compose reports v5.6.0; default Java lookup finds no JDK; Docker daemon socket unavailable; pre-commit absent from PATH |

The README's report publication claims need checking against workflow triggers and deployment conditions. The receipt endpoint in the framework guide is explicitly a hypothetical extension and must remain labelled as such or be replaced by an example using an implemented endpoint.

## Approach

Use full adoption in sequential, reviewable changes: baseline evidence, documentation, namespace/dependencies, gates, final verification. A documentation-only change would leave required namespace and gate gaps. A broad application rewrite would add unrelated risk and obscure the existing showcase.

## Review focus

1. Namespace moves can break TestNG package discovery, Spring scanning, logging categories and documentation links: compile both modules, run all tests and search for old references.
2. Fresh checkout and repeated runs must both work: execute the documented Compose startup and suite, then repeat against the same database.
3. Documentation consolidation can lose operational details: map every existing guide section to its destination and review the final diff against the baseline.
4. Reporting can appear configured while steps, SQL or deployment are broken: inspect generated Allure results/report and manually review deployment conditions.
5. Local hooks can diverge from CI or misparse real configuration: use the same pinned tools, filters and exclusions; check all maintained files and review intentional Markdown hard breaks and TestNG's XML DOCTYPE.

## Task 1: Capture baseline and runnable evidence

**Files:** Record a concise baseline and eventual verification ledger in `docs/development.md`; preserve original docs through Git history and a working audit inventory.

- [x] Confirm the checkout remains clean apart from this plan; inspect applicable repository instructions and use an isolated branch/worktree if execution requires it.
- [x] Locate an existing Java 21 installation or use the project's Java 21 containers; start Docker and verify daemon readiness. Record actual tool versions. Do not claim local Java support from Maven's presence alone.
- [x] Inspect all four test classes, `PaymentApi`, `SigningFilter`, `DbAssertions`, `Database`, `SecurityFilter`, `PaymentService`, controller, schema/seed files, POMs and workflow configurations.
- [x] Inventory every section in `docs/framework-guide.md` and all README claims, commands, source links and image references. Check group filtering behavior before documenting smoke/regression commands.
- [x] Run the current Compose quickstart and suite before changes where the environment permits; capture totals and representative lifecycle/signing/report evidence. If blocked, record the exact blocker and distinguish it from a failing test.

## Task 2: Reorganize documentation without losing showcase value

**Files:** Modify `README.md`; create `AGENTS.md`, `docs/architecture.md`, `docs/development.md`; consolidate `docs/framework-guide.md`; optionally move `docs/images/allure-report.png` to `docs/assets/allure-report.png` with all references updated.

- [x] Use a clear project title such as “Payments API Test Automation” and a short summary explaining the testing problem and why HTTP plus persistence evidence matters. Identify the backend as a bundled demonstration service.
- [x] Preserve and verify source-linked capabilities, coverage/error matrix, root Mermaid diagram, report link and screenshot. Add core tool badges and place the accurate module tree and stack overview beside the architecture.
- [x] Order the README as purpose/capabilities, architecture and structure/stack, quickstart, usage, verification/reporting, documentation index and license. Move exhaustive troubleshooting and configuration to the development guide.
- [x] Map framework-guide design/layer overview, signing canonicalization example, SQL checks, reporting mechanics and API reference into `docs/architecture.md`. Add component boundaries, lifecycle diagrams, concurrency and relevant demo-service limitations verified from code.
- [x] Map framework-guide Docker-free setup, IDE/AspectJ instructions, configuration precedence/table, test-extension example and report commands into `docs/development.md`. Add backend-only settings such as timestamp skew and distinguish backend configuration from test-client overrides.
- [x] Add hooks/CI guidance, expected check scope and the evidence/exception ledger to the development guide. Link versions to manifests; qualify the 14-test output until executed.
- [x] Add `AGENTS.md` with the complete profile, pinned standards URL/SHA, working commands, module boundaries and trust/check rules.
- [x] Review every original section against its destination before removing the superseded guide. Update links, anchors and source references together; preserve active plans as planning records, outside the maintained-guide count.

## Task 3: Align Java structure and dependency reproducibility

**Files:** `pom.xml`, `backend/pom.xml`, `automation/pom.xml`, both modules' Java trees, `automation/src/test/resources/testng-suite.xml`, `backend/src/main/resources/application.yml`, relevant logging/static-analysis configuration, Dockerfiles, docs; add Maven wrapper files if adopted.

- [x] Change Maven group IDs to `com.qaframework`; move Java sources and imports to `com.qaframework.payments.backend` and `com.qaframework.payments.automation`, retaining functional subpackages and module artifact IDs.
- [x] Update TestNG package discovery, logging categories, configuration references, examples and source links. Search for `io.github.yashwantdas` after the move; no active reference should remain.
- [x] Review every direct library, BOM, build plugin and container version against official release/registry evidence. Select stable versions compatible with Java 21 and the whole set; record reviewed versions and any constrained upgrade with its evidence.
- [x] Keep Allure/AspectJ integration aligned, Spring-managed dependencies coherent and Logback classic/core synchronized. Pin lifecycle plugins currently inherited implicitly where required for reproducibility.
- [x] Add a pinned Maven wrapper with distribution verification if practical; use its command consistently locally and in CI. Retain documented container paths for users without a local JDK.
- [x] Compile and run SpotBugs after namespace/dependency changes, then execute the full integration suite. Verify actual discovery includes all four classes and compare results with the baseline.

## Task 4: Add matching local and CI gates

**Files:** Create `.pre-commit-config.yaml`, `.markdownlint-cli2.jsonc` and any narrowly scoped style configuration; modify `.github/workflows/ci.yml`, `.github/workflows/codeql.yml`, `.github/workflows/dependency-review.yml`, `.github/dependabot.yml` and `docs/development.md`.

- [x] Add default Markdown rules with only `MD013` disabled; pin markdownlint-cli2 consistently locally and in CI without turning the Java application into a Node project. Exclude generated output and historical plans, not maintained docs.
- [x] Add pinned, staged-file hooks for Markdown, whitespace/final newlines, conflict markers and applicable YAML/JSON syntax. Preserve intentional Markdown hard breaks. Assess a staged Java formatter/style gate and document any practical exception.
- [x] Keep hooks independent of MySQL/backend after setup. Document `pre-commit install` and `pre-commit run --all-files`; CI must independently check the full maintained scope using the same versions and configuration.
- [x] Resolve all workflow actions to verified full commit SHAs with version comments. Set `persist-credentials: false` on every checkout and 1–3 day artifact retention; retain least permissions and job timeouts.
- [x] Preserve build/static analysis, full controlled API/MySQL tests, CodeQL, dependency review and Allure publication. Review Pages conditions so deployment uses the checked trusted revision only after required gates pass.
- [x] Change Docker Dependabot updates to weekly; retain weekly Maven and Actions updates and useful compatibility groups. Review major changes explicitly.
- [x] Inspect available branch-protection/ruleset and scanning settings without weakening existing enforcement. Record actual enforced check names or pending owner configuration; workflow presence alone is insufficient evidence.
- [x] Prepare a GitHub description of at most 110 characters and at most eight ordered topics grounded in the final implementation. Review current metadata before applying any update during execution.

## Task 5: Validate the final repository and compare with the original

**Files:** Update final evidence in `docs/development.md` and verified presentation in `README.md`; review every changed file.

Run from the repository root; substitute `./mvnw` for `mvn` consistently if the wrapper is adopted:

```bash
pre-commit run --all-files
mvn -B -DskipTests verify
docker compose config --quiet
docker compose up --build -d --wait mysql backend
docker compose --profile test up --build --abort-on-container-exit --exit-code-from tests tests
mvn -B -pl automation allure:report
git diff --check
```

- [ ] Require clean full-file lint/syntax/style results, a successful compile/SpotBugs build and a valid Compose configuration. First-time tool/dependency setup needs network access; these checks do not need running payment services.
- [ ] Require the integration run to execute the intended cases with zero failures/errors/skips; confirm final counts from Surefire XML, not copied README text. Repeat with `docker compose --profile test run --rm --no-deps tests` against healthy existing services to verify isolation and replay-safe signing without dependency restarts.
- [ ] Inspect lifecycle states, negative security cases and SQL/audit attachments in Allure. Confirm AspectJ steps survive namespace/dependency changes; verify report generation and local report-opening instructions.
- [ ] Preserve the existing screenshot unless a newly generated report provides a more accurate example. Review the screenshot and Mermaid rendering manually; do not label an old screenshot as a new run.
- [ ] Check relative links, anchors, source paths, all example endpoints, module hierarchy and configuration defaults. Exercise Docker-free startup and IDE instructions where prerequisites permit; otherwise record the unexecuted scope.
- [ ] Compare the final documents with `31ac98adb4bdbb8224cbf0fc37602a7592a52883` using the section inventory; confirm setup, signing examples, API reference, troubleshooting, extension guidance and report value survived.
- [ ] Clean up only containers/resources created for this verification with `docker compose --profile test down`. Keep generated reports/logs out of Git.
- [ ] Review all changes, summarize verified results and unresolved checks, and prepare a focused PR for owner review. Do not claim GitHub-only checks passed before the PR's CI runs.

## Completion criteria

The project retains its distinctive capabilities and useful documentation, meets the pinned complete-profile requirements or records justified exceptions, has working source/documentation links, and has recorded local verification plus clearly identified GitHub/manual checks. A successful plan review alone is not standards adoption or a passing test run.

## Execution record

Local execution completed on 2026-10-29: Java build/SpotBugs, full-file hooks, fresh Docker suite, two state-preserving Docker repeats and Allure generation passed. Full evidence and explicit exceptions are in `docs/development.md`. All 46 Java files preserve their baseline logic after namespace substitution. The review's missing dependency-plugin pin was fixed; repeat instructions now avoid dependency restarts.

Fresh browser/report and Mermaid rendering remain unverified after automatic browser approval failed at its usage limit. GitHub CI and deployment are verified in their hosted environments; deployment remains a post-merge action. The plan is a historical implementation record, not a source of current setup commands.
