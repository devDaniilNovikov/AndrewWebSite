#!/usr/bin/env bash
# Runs the Spring Boot application and nginx side by side. If either process exits, the
# other is stopped and the container exits non-zero so the platform restarts it instead of
# leaving nginx serving pages while lead delivery is down.
set -uo pipefail

java --enable-native-access=ALL-UNNAMED -Dspring.profiles.active=prod \
    -jar /app/application.jar &
java_pid=$!

nginx -e /dev/stderr -c /etc/nginx/nginx.conf -g 'daemon off;' &
nginx_pid=$!

stop() {
    kill -TERM "$java_pid" "$nginx_pid" 2>/dev/null
    wait
}
trap 'stop; exit 0' TERM INT

wait -n "$java_pid" "$nginx_pid"
stop
exit 1
