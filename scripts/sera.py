"""Instalación, arranque, verificación y reinicio de la demo local de SERA.

Usa Python estándar. Ejecutar desde cualquier directorio con Python 3.10+.
"""
import argparse
from datetime import datetime
import getpass
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
API = ROOT / "apps" / "api"
WEB = ROOT / "apps" / "web"
LOGS = ROOT / ".sera" / "logs"
BASE = "http://127.0.0.1:4500"


def command(name):
    executable = shutil.which(name)
    if executable is None:
        raise RuntimeError(f"Falta {name} en PATH. Consultá docs/ENTREGA.md.")
    return executable


def run(args, cwd=ROOT, **kwargs):
    return subprocess.run(args, cwd=cwd, check=True, **kwargs)


def docker(*args, **kwargs):
    return run([command("docker"), "compose", *args], **kwargs)


def doctor():
    java = run([command("java"), "-version"], capture_output=True, text=True)
    version = java.stderr + java.stdout
    if not re.search(r'version "21\.', version):
        raise RuntimeError("La API requiere Java 21. Revisá JAVA_HOME y PATH.")
    node = run([command("node"), "--version"], capture_output=True, text=True).stdout.strip()
    major, minor, *_ = map(int, node.lstrip("v").split("."))
    if not ((major == 22 and minor >= 13) or major >= 24):
        raise RuntimeError("Node debe ser 22.13+ dentro de la rama 22, o 24+.")
    expected = json.loads((WEB / "package.json").read_text())["packageManager"].split("@")[1]
    actual = run([command("pnpm"), "--version"], capture_output=True, text=True).stdout.strip()
    if actual != expected:
        raise RuntimeError(f"Usá pnpm {expected}, versión declarada por el proyecto. Actual: {actual}.")
    docker("version")
    run([command("docker"), "info"], capture_output=True)
    print(f"Requisitos OK: Java 21, Node {node}, pnpm {actual}, Docker y Compose.", flush=True)


def verify():
    for port in (4500, 4501):
        free_port(port)
    wrapper = API / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    run([str(wrapper), "verify"], cwd=API)
    run([command("pnpm"), "lint"], cwd=WEB)
    run([command("pnpm"), "build"], cwd=WEB)
    docker("config", "--quiet")


def setup():
    doctor()
    for source, destination in [(API / ".env.example", API / ".env"), (WEB / ".env.example", WEB / ".env.local")]:
        if not destination.exists():
            shutil.copyfile(source, destination)
    run([command("pnpm"), "install", "--frozen-lockfile"], cwd=WEB)
    docker("up", "-d", "--wait", "postgres")
    verify()
    print("Instalación lista. Ejecutá: python scripts/sera.py start --demo", flush=True)


def free_port(port):
    with socket.socket() as connection:
        try:
            connection.bind(("127.0.0.1", port))
        except OSError as error:
            raise RuntimeError(f"El puerto {port} está ocupado. Cerrá esa instancia antes de continuar.") from error


def local_environment():
    # Esta automatización solo opera sobre la base local indicada, incluso si .env
    # o la terminal tienen configuración para otra base.
    return {**os.environ, "SPRING_DATASOURCE_URL": "jdbc:postgresql://localhost:5432/sera",
            "SPRING_DATASOURCE_USERNAME": "sera", "SPRING_DATASOURCE_PASSWORD": "sera",
            "SPRING_PROFILES_ACTIVE": "dev", "SPRING_DOCKER_COMPOSE_ENABLED": "false",
            "SERVER_PORT": "4500", "API_PROXY_TARGET": BASE}


def launch(args, cwd, log, env):
    options = {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP} if os.name == "nt" else {"start_new_session": True}
    return subprocess.Popen(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, env=env, **options)


def stop(process):
    if process.poll() is not None:
        return
    if os.name == "nt":
        # Solo procesos hijos iniciados por esta ejecución.
        subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
    else:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
    process.wait(timeout=15)


def wait_ready(url, process, seconds=120):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError("El servicio terminó antes de arrancar. Revisá .sera/logs.")
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(1)
    raise RuntimeError(f"El servicio no respondió a tiempo en {url}. Revisá .sera/logs.")


def password():
    result = os.environ.get("SERA_DEMO_PASSWORD") or getpass.getpass("Contraseña de las cuentas demo, mínimo 8 caracteres: ")
    if len(result) < 8 or len(result.encode()) > 72:
        raise ValueError("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes.")
    return result


def reset_database():
    # No acepta host, volumen ni nombre de base como parámetros. No elimina otras
    # bases alojadas en el mismo contenedor.
    docker("up", "-d", "--wait", "postgres")
    for port in (4500, 4501):
        free_port(port)
    sessions = docker("exec", "-T", "postgres", "psql", "-U", "sera", "-d", "sera", "-At", "-c",
                      "SELECT count(*) FROM pg_stat_activity WHERE datname='sera' AND pid<>pg_backend_pid();",
                      capture_output=True, text=True).stdout.strip()
    if sessions != "0":
        raise RuntimeError("La base sera tiene conexiones activas. Cerrá la API y las herramientas de base antes de reiniciar.")
    backup_root = Path(os.environ.get("LOCALAPPDATA", str(Path.home() / ".local" / "share"))) / "SERA" / "backups"
    backup_root.mkdir(parents=True, exist_ok=True)
    backup = backup_root / f"sera-{datetime.now():%Y%m%d-%H%M%S-%f}.dump"
    with backup.open("wb") as output:
        docker("exec", "-T", "postgres", "pg_dump", "-U", "sera", "-d", "sera", "-Fc", stdout=output)
    if backup.stat().st_size == 0:
        raise RuntimeError("El respaldo está vacío. No se reinició la base.")
    print(f"Respaldo guardado en {backup}", flush=True)
    docker("exec", "-T", "postgres", "psql", "-U", "sera", "-d", "sera", "-v", "ON_ERROR_STOP=1", "-c",
           "BEGIN; DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION sera; COMMIT;")
    print("Base local sera reiniciada. Flyway creará el esquema durante el arranque.", flush=True)


def start(demo_password=None, once=False, with_web=True):
    jars = list((API / "target").glob("sera-*.jar"))
    if len(jars) != 1:
        raise RuntimeError("Primero ejecutá setup o verify para generar el JAR.")
    free_port(4500)
    if with_web and not once:
        free_port(5173)
    docker("up", "-d", "--wait", "postgres")
    LOGS.mkdir(parents=True, exist_ok=True)
    children = []
    env = local_environment()
    try:
        with (LOGS / "api.log").open("w", encoding="utf-8") as api_log, (LOGS / "web.log").open("w", encoding="utf-8") as web_log:
            api = launch([command("java"), "-Duser.timezone=UTC", "-jar", str(jars[0])], API, api_log, env)
            children.append(api)
            wait_ready(BASE + "/api/health", api)
            if demo_password:
                from demo import seed
                seed("sera", demo_password, BASE)
            if once:
                print("Migraciones y demo verificadas; API detenida al finalizar.", flush=True)
                return
            if with_web:
                web = launch([command("pnpm"), "dev", "--host", "127.0.0.1", "--port", "5173", "--strictPort"], WEB, web_log, env)
                children.append(web)
                wait_ready("http://127.0.0.1:5173", web, 60)
            print("SERA disponible: http://localhost:5173 · API: http://localhost:4500\nLogs en .sera/logs. Ctrl+C detiene los servicios de esta ejecución.", flush=True)
            while all(p.poll() is None for p in children):
                time.sleep(1)
            raise RuntimeError("Un servicio terminó. Revisá .sera/logs.")
    except KeyboardInterrupt:
        print("Deteniendo API y frontend…", flush=True)
    finally:
        for child in reversed(children):
            stop(child)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["doctor", "setup", "verify", "start", "reset-demo"])
    parser.add_argument("--demo", action="store_true", help="Carga la demo al arrancar, sin borrar datos")
    parser.add_argument("--confirm", help="Para reset-demo, debe ser exactamente REINICIAR-SERA-LOCAL")
    parser.add_argument("--once", action="store_true", help="Verifica arranque/demo y luego detiene la API")
    args = parser.parse_args()
    if args.action == "doctor":
        doctor()
    elif args.action == "setup":
        setup()
    elif args.action == "verify":
        verify()
    elif args.action == "reset-demo":
        if args.confirm != "REINICIAR-SERA-LOCAL":
            parser.error("reset-demo requiere --confirm REINICIAR-SERA-LOCAL. Borra los datos de la base local sera y guarda un respaldo.")
        demo_password = password()
        # Validar artefactos y puertos antes del cambio destructivo.
        if len(list((API / "target").glob("sera-*.jar"))) != 1:
            raise RuntimeError("Ejecutá setup o verify antes de reiniciar la base.")
        if not args.once:
            free_port(5173)
        reset_database()
        start(demo_password, args.once)
    else:
        start(password() if args.demo else None, args.once)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, subprocess.CalledProcessError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)
