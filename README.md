# IntelliRide — Agentic AI Ride-Hailing Backend

IntelliRide is a **complete ride-hailing backend** — riders, drivers, the full ride lifecycle
(request → match → OTP start → end → pay → rate), wallets, and ratings — exposed as a plain,
authenticated **REST API**. The whole thing runs end-to-end with the LLM involved in *no* request;
see [The core ride-hailing backend](#the-core-ride-hailing-backend-works-without-the-ai-layer).

On top of that foundation sits an **agentic AI layer**: a multi-agent system that routes requests to
specialized agents, **plans and executes multi-step goals autonomously**, asks for human confirmation
before changing data, and returns a full **reasoning trace** of every decision. Everything the AI can
do is also a direct REST call — the AI is an *augmentation*, not the application.

The AI layer demonstrates five building blocks working together:

| Concept | What it does here | Key code |
|--------|-------------------|----------|
| **LLM** | Natural-language understanding & generation (OpenAI `gpt-4o`) | `AIConfig`, per-agent `ChatClient`s |
| **RAG** | Grounds policy/FAQ answers in your own docs via a pgvector store | `RAGService`, `QuestionAnswerAdvisor`, `resources/knowledge/*.md` |
| **AI Agent** | LLMs that *decide which tools to call* and execute them (book/cancel/rate, …) | `RiderTools`, `DriverTools`, `RiderAgent`, `DriverAgent` |
| **Agentic orchestration** | A supervisor that **routes**, **plans** multi-step goals, **confirms** actions, and **resumes** paused plans | `SupervisorService`, `PlannerService`, `PendingPlanStore`, `agents/` |
| **MCP** | Exposes the same tools over the Model Context Protocol so external clients (e.g. Claude Desktop) can drive the app | `McpConfig`, MCP WebMVC/SSE server |

---

## Tech stack

- **Java 21**, **Spring Boot 4.x**
- **Spring AI 2.0** — `ChatClient`, advisors, tool calling, **structured output**, pgvector vector store, MCP server
- **OpenAI** — `gpt-4o` (chat) + `text-embedding-3-small` (embeddings)
- **PostgreSQL + PostGIS** (spatial) + **pgvector** (RAG vector store)
- **Spring Security + JWT**, Redis, OSRM (distance), ModelMapper

---

## The core ride-hailing backend (works without the AI layer)

Underneath the agentic layer, IntelliRide is a full ride-hailing REST API. Every action the assistant
performs is also available as a plain authenticated REST call, and the whole ride-hailing API runs
without the LLM being involved in any request.

### Ride lifecycle

```
rider requests   →   driver accepts        →   driver starts (OTP)      →   driver ends           →   both rate
(RideRequest,        (Ride + 4-digit OTP        (verify OTP; Payment         (fare finalized,          (rateDriver /
 PENDING)             created, CONFIRMED)         PENDING + Rating row)        wallet settled,           rateRider)
                                                                               30% commission)
```

Status flow — `RideRequest`: `PENDING → CONFIRMED`; `Ride`: `CONFIRMED → ONGOING → ENDED` (or `CANCELLED`).

### Direct REST API

| Area | Endpoints |
|------|-----------|
| **Auth** *(public)* | `POST /auth/signup` (creates a RIDER), `POST /auth/login` (→ JWT), `POST /auth/mcp-token`, `POST /auth/onBoardNewDriver/{userId}` *(ROLE_ADMIN)* |
| **Rider** *(`ROLE_RIDER`)* | `POST /rider/requestRide`, `POST /rider/cancelRide/{rideId}`, `POST /rider/rateDriver`, `GET /rider/getMyProfile`, `GET /rider/getMyRides` |
| **Driver** *(`ROLE_DRIVER`)* | `POST /drivers/acceptRide/{rideRequestId}`, `POST /drivers/startRide/{rideId}`, `POST /drivers/endRide/{rideId}`, `POST /drivers/cancelRide/{rideId}`, `POST /drivers/rateRider`, `GET /drivers/getMyProfile`, `GET /drivers/getMyRides` |

Responses are wrapped by `GlobalResponseHandler` as `{ "data": ..., "error": ... }`, and the acting
user is always resolved from the JWT — endpoints never take a caller id.

### Domain model & business rules

- **Entities** — `User` (roles: RIDER / DRIVER / ADMIN), `Rider`, `Driver`, `RideRequest`, `Ride`,
  `Payment`, `Wallet` + `WalletTransaction` (ledger), `Rating`.
- **Strategy pattern** (`strategies/`) — rules chosen at runtime by a `RideStrategyManager`:
  - *Fare* — default vs. **surge pricing** during peak hours (18:00–21:00).
  - *Driver matching* — **nearest** driver, or **highest-rated** for riders rated ≥ 4.8.
- **Payments & wallet** — `CASH` or `WALLET`; settlement debits the rider, credits the driver (minus
  a **30% platform commission**), and writes `WalletTransaction` ledger rows.
- **Idempotent, concurrency-safe settlement** — `endRide` → payment is guarded by optimistic locking
  (`@Version` on `Ride`) and a unique ledger key, so a retried or concurrent `endRide` can never
  double-charge (details in [`docs/IDEMPOTENCY_CHANGES.md`](docs/IDEMPOTENCY_CHANGES.md)).
- **Geospatial** — pickup/drop-off stored as **PostGIS** points (SRID 4326); road distance via **OSRM**.
- **Security** — stateless **JWT**; method-level `@Secured` role checks; ownership/status validated
  server-side in the domain services.

---

## The agentic layer (what makes it more than a chatbot)

Every `/ai/chat` request is handled by **`SupervisorService`**, which picks one of three paths:

1. **Direct route** (simple request) — rule-based routing sends the message to the right agent and
   returns the answer in one step.
2. **Plan & execute** (multi-step goal) — `PlannerService` decomposes the goal into a typed `Plan`
   (Spring AI **structured output**), and the supervisor runs the steps in a loop.
3. **Resume** — if a plan is paused awaiting confirmation, the message is treated as the yes/no reply.

**Specialized agents** — each is its own `ChatClient` with its own system prompt, tools, and advisors:

| Agent | Tools | Advisors |
|-------|-------|----------|
| `RiderAgent` | `RiderTools` | chat memory |
| `DriverAgent` | `DriverTools` | chat memory |
| `SupportAgent` | *(none)* | RAG (`QuestionAnswerAdvisor`) |

**Human-in-the-loop (two layers):**
- **Plan-level** — the supervisor pauses on any data-changing step, saves state in `PendingPlanStore`,
  and asks *"Shall I proceed?"*; it resumes on the next request.
- **Tool-level** — each action tool previews with `confirmed=false` and only mutates on `confirmed=true`.

**Reasoning trace** — a shared list every layer appends to (route, plan, delegate, tool call,
confirmation). It's returned in the `reasoning` field of every response, making the agent's
decisions fully observable.

> **Safety guards:** a `MAX_STEPS` cap rejects runaway plans, and the planner falls back to a single
> safe read-only step if the LLM returns a malformed plan.

---

## Architecture
<img width="672" height="548" alt="Screenshot 2026-06-21 at 15 37 39" src="https://github.com/user-attachments/assets/469ef636-13a6-424e-ad0a-5aa7f81c2768" />


> **Note:** the **MCP path bypasses the agentic layer** — when Claude Desktop calls a tool it hits
> `RiderTools`/`DriverTools` directly, so *its* model is the orchestrator. The supervisor/planner/agents
> only run behind the HTTP `/ai/chat` endpoint.

### How a multi-step goal flows (plan + confirm + resume)

```mermaid
sequenceDiagram
    participant User
    participant Sup as SupervisorService
    participant Plan as PlannerService
    participant Agent as RiderAgent
    participant LLM as gpt-4o

    User->>Sup: "cancel my last ride and rate that driver 5 stars"
    Sup->>Plan: decompose goal
    Plan->>LLM: structured-output request
    LLM-->>Plan: Plan[ locate ride, cancel(confirm), rate(confirm) ]
    Sup->>Agent: step 1 — locate ride (read-only)
    Agent-->>Sup: result
    Sup-->>User: "I'm about to: cancel the ride. Proceed? (yes/no)"   %% pause
    User->>Sup: "yes"
    Sup->>Agent: step 2 — cancel (confirmed)
    Agent-->>Sup: result
    Sup-->>User: "I'm about to: rate the driver. Proceed? (yes/no)"   %% pause again
    User->>Sup: "yes"
    Sup->>Agent: step 3 — rate (confirmed)
    Sup-->>User: final synthesized answer + full reasoning trace
```

---

## The building blocks, in detail

### 1. LLM
`AIConfig` provides the shared `ChatClient.Builder`, `ChatMemory` (JDBC-backed), and `VectorStore`
beans. Each agent builds its **own** `ChatClient` from the builder with a focused system prompt.

### 2. RAG (Retrieval-Augmented Generation)
- Policy/FAQ documents live in `src/main/resources/knowledge/*.md`.
- `RAGService` chunks + embeds them into the **pgvector** `vector_store`.
- `SupportAgent` uses a `QuestionAnswerAdvisor` to retrieve the most relevant chunks per query and
  ground its answers in *your* data instead of hallucinating.

### 3. AI Agents (tool calling)
The Rider/Driver agents are `ChatClient`s bound to their tools; the LLM autonomously decides which
`@Tool` to invoke:

- **`RiderTools`** — `requestRide`, `rider_cancelRide`, `rateDriver`, `getWalletBalance`, `rider_getMyRides`, `rider_getMyProfile`
- **`DriverTools`** — `acceptRide`, `startRide`, `endRide`, `driver_cancelRide`, `rateRider`, `setAvailability`, …

Design principles baked in:
- **Tools never take the acting user's id** — identity is resolved server-side from the JWT, so the
  model can't act as someone else.
- **Confirmation gate** — mutating tools require a `confirmed=true` second call.
- **Server-side authorization is the real guarantee** — ownership/status checks live in the domain services.
- **Audit logging** — tool invocations log `TOOL <name> <outcome> user=<id>`.

### 4. Agentic orchestration
- **`SupervisorService`** — routes (rule-based), decides plan-vs-direct, runs the execution loop,
  handles confirmation/resume, and synthesizes the final answer.
- **`PlannerService`** — turns a goal into a typed `PlanDto` via `.entity(PlanDto.class)`; each step
  carries its `targetAgent` and a `requiresConfirmation` flag.
- **`PendingPlanStore`** — holds paused plans (in-memory) keyed by `conversationId` for cross-request resume.
- **Reasoning trace** — `ReasoningStepDto` entries (`agent`, `type`, `detail`, `timestamp`) returned
  on every response.

### 5. MCP (Model Context Protocol)
`McpConfig` exposes the same tools over an MCP SSE server (`/sse` + `/mcp/message`). An external MCP
client (Claude Desktop, MCP Inspector) connects and uses the tools — **its** model becomes the agent.

- **Auth**: the MCP connection carries a long-lived JWT via `Authorization: Bearer`; `JwtAuthFilter`
  resolves the user so per-user rules still apply. Mint one with `POST /auth/mcp-token` (valid 30 days).
- Tool names are globally unique (`rider_*` / `driver_*`) for MCP's flat namespace.

---

## Running locally

### Prerequisites
- Java 21, Maven (wrapper included), PostgreSQL with **PostGIS** and **pgvector** extensions, Redis
- An OpenAI API key

### Configuration
Secrets are read from the environment — **do not commit them**:
```bash
export OPENAI_API_KEY=sk-...
```
`application.yaml` references `${OPENAI_API_KEY}`.

### Start
```bash
./mvnw spring-boot:run
```
> `spring.jpa.hibernate.ddl-auto=create-drop` — the database is **recreated on every restart**, then
> `data.sql` seeds demo data.

### Create a test rider and talk to the assistant
```bash
# 1. sign up a rider (signup always creates a RIDER)
curl -s -X POST localhost:8080/auth/signup -H 'Content-Type: application/json' \
  -d '{"name":"Test Rider","email":"testrider@example.com","password":"Test@1234"}'

# 2. login -> JWT access token (valid 10 min)
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"testrider@example.com","password":"Test@1234"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 3. chat — the response includes a `reasoning` trace of which agent/planner ran
curl -s -X POST localhost:8080/ai/chat -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"message":"cancel my most recent ride and then rate that driver 5 stars"}'
```

Sample response shape:
```json
{
  "data": {
    "reply": "I'm about to: Cancel the most recent ride. Shall I proceed? (yes/no)",
    "conversationId": "user-43",
    "reasoning": [
      { "agent": "PLANNER",    "type": "PLAN",         "detail": "Plan with 3 step(s): [...]" },
      { "agent": "RIDER",      "type": "DELEGATE",     "detail": "RiderAgent handling: ..." },
      { "agent": "SUPERVISOR", "type": "CONFIRMATION", "detail": "Awaiting confirmation for: ..." }
    ]
  },
  "error": null
}
```
Reply **"yes"** in a follow-up request (same token → same `conversationId`) to resume the plan.

### Drive a full ride with plain REST (no AI)

The same flow the assistant automates, done directly — no LLM involved. Uses the seeded test accounts
(`testrider@uber.com` / `testdriver@uber.com`, password `Test@1234`):

```bash
# rider + driver access tokens
RTOK=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"testrider@uber.com","password":"Test@1234"}' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
DTOK=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"testdriver@uber.com","password":"Test@1234"}' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 1. rider requests a ride (returns the RideRequest id)
curl -s -X POST localhost:8080/rider/requestRide -H "Authorization: Bearer $RTOK" \
  -H 'Content-Type: application/json' \
  -d '{"pickupLocation":{"type":"Point","coordinates":[77.2090,28.6139]},
       "dropOffLocation":{"type":"Point","coordinates":[77.2500,28.6500]},"paymentMethod":"WALLET"}'

# 2. driver accepts it → returns the ride id + OTP
curl -s -X POST localhost:8080/drivers/acceptRide/1 -H "Authorization: Bearer $DTOK"

# 3. driver starts the ride with the rider's OTP  (creates the PENDING payment)
curl -s -X POST localhost:8080/drivers/startRide/1 -H "Authorization: Bearer $DTOK" \
  -H 'Content-Type: application/json' -d '{"otp":"<otp-from-step-2>"}'

# 4. driver ends the ride → fare finalized, wallets settled (idempotent + concurrency-safe)
curl -s -X POST localhost:8080/drivers/endRide/1 -H "Authorization: Bearer $DTOK"
```

### Connect from Claude Desktop (MCP)
`~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "intelliride": {
      "command": "/opt/homebrew/bin/npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/sse", "--transport", "sse-only",
               "--header", "Authorization:${AUTH_HEADER}"],
      "env": { "AUTH_HEADER": "Bearer <token from /auth/mcp-token>" }
    }
  }
}
```
Use the **30-day MCP token** (`POST /auth/mcp-token`), not the 10-minute access token, so you don't
have to restart Claude when it expires. Requires Node 18+.

---

## Key endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/auth/signup`, `/auth/login` | Auth (returns JWT) |
| POST | `/auth/mcp-token` | Mint a 30-day token for MCP clients |
| POST | `/ai/chat` | Talk to the agentic assistant (`{ "message": "..." }`) — returns `reply` + `reasoning` |
| POST/GET | `/rider/*` | Rider actions — request/cancel/rate, profile, rides ([details](#the-core-ride-hailing-backend-works-without-the-ai-layer)) |
| POST/GET | `/drivers/*` | Driver actions — accept/start/end/cancel/rate, profile, rides ([details](#the-core-ride-hailing-backend-works-without-the-ai-layer)) |
| GET/POST | `/sse`, `/mcp/message` | MCP server (SSE transport) |
| GET | `/swagger-ui.html` | API docs |

`/ai/chat` derives `conversationId` server-side as `user-<id>` from the JWT — the request body is just
`{ "message": "..." }`. All responses are wrapped by `GlobalResponseHandler` as `{ "data": ..., "error": ... }`.

---

## Security notes
- The MCP token is long-lived (30 days) — treat it like an API key and make it revocable before
  exposing the server publicly.

---

## Project layout

```
src/main/java/com/flourish/intelliride/
├── agents/         Agent, AgentRequest, AgentResult, RiderAgent, DriverAgent, SupportAgent
├── configs/        AIConfig (ChatClient/memory/vector beans), McpConfig, WebSecurityConfig, MapperConfig
├── controllers/    AuthController, RiderController, DriverController, AIController
├── tools/          RiderTools, DriverTools          ← agent tools (@Tool)
├── services/       SupervisorService, PlannerService, PendingPlanStore, RAGService + domain services
├── strategies/     fare (default/surge) + driver-matching strategies
├── security/       JwtAuthFilter, JWTService
├── advices/        GlobalResponseHandler, GlobalExceptionHandler
├── dtos/           ChatResponseDto, ReasoningStepDto, PlanDto, PlanStepDto, PendingPlanDto, …
├── entities/       + enums/ (AgentType, StepType, StepStatus, Role, …)
└── resources/
    ├── knowledge/  RAG source docs (fares, wallet, cancellation, faq)
    ├── application.yaml
    └── data.sql    demo seed data
```
