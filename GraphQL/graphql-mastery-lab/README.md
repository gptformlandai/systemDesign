# GraphQL Mastery Lab

> Hands-on companion for the GraphQL Mastery track.

This lab turns the study sheets into repeatable schema, operation, resolver, debugging, governance, and interview practice.

## Lab Map

| Area | Purpose |
|---|---|
| [LEARNING_PATH.md](LEARNING_PATH.md) | ordered lab sequence |
| [SCRIPTS](SCRIPTS) | safe read-only GraphQL helper scripts |
| [EXAMPLES/simple-store](EXAMPLES/simple-store) | dependency-free schema, operations, and resolver simulation |
| [LABS](LABS) | guided hands-on exercises |
| [PROJECTS](PROJECTS) | portfolio-grade GraphQL projects |
| [CHEATSHEETS](CHEATSHEETS) | fast schema/query/debug recall |
| [INTERVIEW_PREP](INTERVIEW_PREP) | senior answer patterns and questions |
| [RUNBOOKS](RUNBOOKS) | incident response playbooks |

## Recommended Flow

1. Read [../GraphQL-Mastery-Sheet-System.md](../GraphQL-Mastery-Sheet-System.md).
2. Complete the foundation and intermediate sheets in order.
3. Explore [EXAMPLES/simple-store](EXAMPLES/simple-store).
4. Run the scripts in [SCRIPTS](SCRIPTS) against the example files.
5. Complete the labs, projects, and runbooks.
6. Use interview prep to turn implementation knowledge into clear answers.

## Safety Rules

- Use sample schemas and operation files for practice.
- Do not paste production tokens, queries, or customer data into lab files.
- Treat operation documents as potentially sensitive when they include field names from private domains.
- Keep auth and tenant scope explicit in every resolver design.

## Minimum Tools

- Bash-compatible shell
- Optional: Node.js for the resolver simulation
- Optional: GraphQL server/client tooling if you want to turn the examples into a running API

## Lab Completion Definition

You have completed this lab when you can explain:

```text
How a GraphQL schema is designed, queried, resolved, authorized, batched, cached, observed, evolved, federated, and debugged.
```