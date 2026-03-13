# Easy Startup Commands

This page is a quick command map for starting and stopping RiskManagement Pro in both local-native mode and Docker mode.

## Quick Decision Table

| I want to... | Run this command |
|---|---|
| Start local-native mode | `./easy.sh local-start` |
| Stop local-native mode | `./easy.sh local-stop` |
| Start Docker mode | `./easy.sh docker-start` |
| Stop Docker mode | `./easy.sh docker-stop` |
| See command help | `./easy.sh --help` |

## Recommended on Windows

Use Git Bash executable directly from PowerShell:

```powershell
& "C:\Program Files\Git\bin\bash.exe" ./easy.sh local-start
& "C:\Program Files\Git\bin\bash.exe" ./easy.sh local-stop
& "C:\Program Files\Git\bin\bash.exe" ./easy.sh docker-start
& "C:\Program Files\Git\bin\bash.exe" ./easy.sh docker-stop
```

## Linux or macOS

```bash
./easy.sh local-start
./easy.sh local-stop
./easy.sh docker-start
./easy.sh docker-stop
```

## What Each Command Does

1. `local-start`
- Starts backend and frontend as host processes (no Docker).
- Uses local PostgreSQL.

2. `local-stop`
- Stops host-native backend and frontend processes started by local mode.

3. `docker-start`
- Starts postgres, backend, and frontend using compose.
- Frontend URL: http://localhost
- Backend URL: http://localhost:8080/api/loans

4. `docker-stop`
- Stops and removes compose containers for this project.

## Notes

1. If Docker mode fails with port conflict on `8080`, stop local mode first:

```powershell
& "C:\Program Files\Git\bin\bash.exe" ./easy.sh local-stop
```

2. If `bash ./easy.sh` fails with `/bin/bash not found`, use Git Bash path explicitly (Windows command block above).

3. Backend API may return `403` when accessed directly without login token. This means service is up and auth is enforced.
