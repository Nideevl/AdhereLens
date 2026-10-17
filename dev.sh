#!/usr/bin/env bash
# One command to run the whole AdhereLens stack in the background.
#   ./dev.sh start          start Docker, the 3 services and the frontend (builds first if needed)
#   ./dev.sh start --build  same, but rebuild the jars first (use after changing backend code)
#   ./dev.sh stop           stop the 4 services and the frontend (Docker keeps running)
#   ./dev.sh status         show what is up
#   ./dev.sh logs <name>    follow a log: identity | clinical | notification | analytics | frontend
set -u
cd "$(dirname "$0")"
RUN=.run
mkdir -p "$RUN"

SERVICES="identity:identity-service:8081 clinical:clinical-service:8082 notification:notification-service:8083 analytics:analytics-service:8084"

port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

wait_port() { # name port seconds
  local i=0
  while ! port_open "$2"; do
    i=$((i + 1))
    if [ "$i" -gt "$3" ]; then echo "  $1: not up on port $2 after $3s. Check $RUN/$1.log"; return 1; fi
    sleep 1
  done
  echo "  $1: up on port $2"
}

jar_of() { ls "$1"/target/*-SNAPSHOT.jar 2>/dev/null | head -n 1; }

build() {
  echo "Building backend..."
  mvn -q clean install -DskipTests || { echo "Build failed"; exit 1; }
}

start_one() { # name module port
  local name="$1" module="$2" port="$3" jar
  if port_open "$port"; then echo "  $name: already running on port $port"; return; fi
  jar=$(jar_of "$module")
  if [ -z "$jar" ]; then echo "  $name: no jar found, run ./dev.sh start --build"; return; fi
  nohup java -jar "$jar" > "$RUN/$name.log" 2>&1 &
  echo $! > "$RUN/$name.pid"
}

stop_one() { # name
  local pidfile="$RUN/$1.pid"
  if [ -f "$pidfile" ]; then
    kill "$(cat "$pidfile")" 2>/dev/null && echo "  stopped $1"
    rm -f "$pidfile"
  fi
}

case "${1:-}" in
  start)
    if [ "${2:-}" = "--build" ] || [ -z "$(jar_of clinical-service)" ]; then build; fi
    echo "Starting Docker (Postgres, Kafka)..."
    docker compose up -d || { echo "Docker failed"; exit 1; }
    wait_port postgres 5432 60 || exit 1
    wait_port kafka 9092 60 || exit 1
    echo "Starting services..."
    for s in $SERVICES; do IFS=: read -r name module port <<< "$s"; start_one "$name" "$module" "$port"; done
    for s in $SERVICES; do IFS=: read -r name module port <<< "$s"; wait_port "$name" "$port" 120; done
    echo "Starting frontend..."
    if ! port_open 5173; then
      [ -d frontend/node_modules ] || (cd frontend && npm install --no-audit --no-fund)
      (cd frontend; nohup npm run dev > "../$RUN/frontend.log" 2>&1 & echo $! > "../$RUN/frontend.pid")
    fi
    wait_port frontend 5173 60
    echo
    echo "Ready. Open the forwarded port 5173."
    ;;
  stop)
    for s in $SERVICES; do IFS=: read -r name module port <<< "$s"; stop_one "$name"; done
    # npm starts a child process, so stop whatever is still listening on 5173
    stop_one frontend
    pkill -f "node .*vite" 2>/dev/null && echo "  stopped frontend"
    echo "Docker is still running. Use: docker compose down"
    ;;
  status)
    for s in $SERVICES; do IFS=: read -r name module port <<< "$s"; if port_open "$port"; then echo "up    $name ($port)"; else echo "DOWN  $name ($port)"; fi; done
    if port_open 5173; then echo "up    frontend (5173)"; else echo "DOWN  frontend (5173)"; fi
    if port_open 5432; then echo "up    postgres (5432)"; else echo "DOWN  postgres (5432)"; fi
    if port_open 9092; then echo "up    kafka (9092)"; else echo "DOWN  kafka (9092)"; fi
    ;;
  logs)
    [ -n "${2:-}" ] && [ -f "$RUN/$2.log" ] && tail -n 80 -f "$RUN/$2.log" || echo "Usage: ./dev.sh logs identity|clinical|notification|analytics|frontend"
    ;;
  *)
    sed -n '2,8p' "$0"
    ;;
esac
