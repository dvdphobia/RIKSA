# Backend repository guidance

- Treat Java source and Flyway migrations as the authority for runtime behavior, API contracts, and schema. Update the matching active document under docs/ when a public contract changes.
- Keep backend code within the existing com.riksa.<feature> package structure. Follow nearby code and tests when extending a feature.
- Make every schema change in a new Flyway migration named V{N}__description.sql; Hibernate validates the schema and does not create it.
- Start with the root README and BACKEND_SETUP.md for setup. Use docs/README.md to find active contracts and design references.
- Run focused tests for changed behavior and the Maven test suite when the environment permits. Report skipped environment-dependent tests explicitly.