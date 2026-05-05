# china-kekao frontend

React 18 + Vite + TypeScript SPA. Implements the skeleton from TASK-017.

## Stack

- Vite 5, React 18, TypeScript (strict)
- React Router for client-side routing
- TanStack Query for server state
- Zustand (with `localStorage` persistence) for auth tokens / current user
- Axios client with automatic access-token refresh on `401`
- Tailwind CSS + shadcn-style base components (`Button`, `Input`, `Card`, `Dialog`)

## Routes

| Path           | Auth required | Notes                          |
|----------------|---------------|--------------------------------|
| `/login`       | no            | Sign-in form (TASK-018)        |
| `/register`    | no            | Registration form (TASK-018)   |
| `/`            | yes           | Dashboard (TASK-019)           |
| `/study`       | yes           | Session player (TASK-020/023)  |
| `/decks`       | yes           | Browse + subscribe (TASK-028)  |
| `/hanzi/:id`   | yes           | Character detail (TASK-027)    |
| `/search`      | yes           | Lookup (TASK-026)              |
| `/stats`       | yes           | Charts (TASK-030)              |
| `/settings`    | yes           | User preferences (TASK-029)    |

Protected routes redirect to `/login` if no access token is present.

## Local development

```bash
cp .env.example .env.local        # optional — defaults work via Vite proxy
npm install
npm run dev                       # http://localhost:5173, /api proxied to :8080
```

`VITE_API_PROXY` controls where the dev server forwards `/api/*`. In Docker
compose this is wired to `http://backend:8080`.

## Production build

```bash
npm run build                     # outputs dist/
npm run preview
```

The Dockerfile has `dev`, `build`, and `prod` (nginx) targets; compose uses
`dev` so HMR works against a mounted source tree.

## Auth flow

1. `useAuthStore` keeps `{ user, tokens }` in `localStorage` (`kekao.auth`).
2. `src/api/client.ts` injects `Authorization: Bearer <accessToken>` on every
   request.
3. On `401`, the response interceptor calls `POST /api/auth/refresh` exactly
   once (concurrent 401s share the in-flight refresh promise), updates the
   store, and replays the original request. If refresh fails the session is
   cleared and protected routes redirect to `/login`.
4. Refresh attempts are skipped for `auth/login`, `auth/register`,
   `auth/refresh` themselves to avoid infinite loops.
