"""Carga datos ficticios de demo en la API y PostgreSQL locales. No borra datos."""
import argparse
import getpass
import http.cookiejar
import json
import os
import subprocess
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class Client:
    def __init__(self, base="http://localhost:4500"):
        self.base = base
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, path, method="GET", data=None, expected=200):
        headers = {"Accept": "application/json"}
        if method != "GET":
            token = self.call("/auth/csrf")
            headers[token["headerName"]] = token["token"]
        if data is not None:
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(self.base + "/api" + path, data=json.dumps(data).encode() if data is not None else None, headers=headers, method=method)
        try:
            response = self.opener.open(req, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read().decode()
        if response.status != expected:
            raise RuntimeError(f"{method} {path}: esperado {expected}, recibido {response.status}: {body}")
        return json.loads(body) if body else None

    def login(self, email, password):
        return self.call("/auth/login", "POST", {"email": email, "password": password})


def seed(database, password, base="http://localhost:4500"):
    if database not in ("sera", "sera_entrega_i3"):
        raise ValueError("La demo solo admite las bases locales sera y sera_entrega_i3.")
    from urllib.parse import urlparse
    target = urlparse(base)
    if target.scheme != "http" or target.hostname not in ("localhost", "127.0.0.1") or target.port != 4500:
        raise ValueError("La demo solo se carga contra la API local en puerto 4500.")
    client = Client(base)
    accounts = [("admin@sera.local", "Administración Demo", 99000001), ("socio@sera.local", "Estudiante Demo", 99000002), ("staff@sera.local", "Accesos Demo", 99000003)]
    for email, name, dni in accounts:
        try:
            user = client.login(email, password)
        except RuntimeError:
            user = client.call("/auth/registro", "POST", {"nombreCompleto": name, "email": email, "dni": dni, "password": password, "relacionUnse": "ESTUDIANTE"}, 201)
        import uuid
        uid = str(uuid.UUID(user["id"]))
        verified = subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", database, "-At", "-v", "ON_ERROR_STOP=1", "-c", f"SELECT count(*) FROM usuarios WHERE id='{uid}';"], cwd=ROOT, check=True, capture_output=True, text=True)
        if verified.stdout.strip() != "1":
            raise RuntimeError("La API no está conectada a la base local indicada. No se asignaron permisos ni se cargó la demo.")
    # Bootstrap restricted to the fictitious administrator in local Docker.
    subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", database, "-v", "ON_ERROR_STOP=1", "-c", "UPDATE usuarios SET rol='ADMIN' WHERE email='admin@sera.local';"], cwd=ROOT, check=True, capture_output=True)
    subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", database, "-v", "ON_ERROR_STOP=1", "-c", "UPDATE usuarios SET rol='STAFF' WHERE email='staff@sera.local';"], cwd=ROOT, check=True, capture_output=True)
    client.login("admin@sera.local", password)
    levels = client.call("/niveles-membresia?soloDisponibles=false")
    if not any(n["nombre"] == "Comunidad UNSE" for n in levels):
        client.call("/niveles-membresia", "POST", {"nombre": "Comunidad UNSE", "descripcion": "Plan mensual del polideportivo. Beneficios sujetos a pago aprobado.", "beneficios": ["Acceso a actividades del polideportivo", "Tarifas según relación con la UNSE"], "disponibleParaContratar": True, "preciosPorRelacion": {"ESTUDIANTE": 8000, "DOCENTE": 12000, "NO_DOCENTE": 10000, "GRADUADO": 14000, "EXTERNO": 18000, "VISITANTE": 20000}}, 201)
    existing = client.call("/espacios")
    for name, kind, capacity, rate, image in [("Cancha cubierta", "Cancha", 20, 12000, "/poli1.jpg"), ("Quincho del polideportivo", "Recreación", 30, 9000, "/poli2.jpg"), ("Pileta", "Natación", 40, 5000, "/poli3.jpg")]:
        space = next((s for s in existing if s["nombre"] == name), None)
        if space is None:
            space = client.call("/espacios", "POST", {"nombre": name, "descripcion": f"{name} del Polideportivo UNSE. Datos de demostración.", "capacidad": capacity, "tarifaHora": rate, "tipo": kind, "rutaImagen": image}, 201)
        sid = space["id"]
        schedules = client.call(f"/espacios/{sid}/disponibilidades")
        for day in ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"]:
            if not any(h["diaSemana"] == day for h in schedules):
                client.call(f"/espacios/{sid}/disponibilidades", "POST", {"diaSemana": day, "horaDesde": "08:00", "horaHasta": "22:00"}, 201)
        client.call(f"/espacios/{sid}/tarifas", "PUT", {"tarifas": {"ESTUDIANTE": rate / 2, "DOCENTE": rate * .75, "EXTERNO": rate}})
    prepare_feedback(client, database, password, base)
    print("Demo preparada: admin@sera.local, socio@sera.local y staff@sera.local. Usan la contraseña indicada.", flush=True)


def prepare_feedback(admin, database, password, base):
    """Fixture local de una reserva histórica, con pago en efectivo y sin cobros externos."""
    from datetime import datetime, timedelta, timezone
    import uuid
    member = Client(base)
    member.login("socio@sera.local", password)
    levels = member.call("/niveles-membresia")
    level = next(n for n in levels if n["nombre"] == "Comunidad UNSE")
    socio = member.call("/socios/me")
    if socio.get("membresiaId") is None:
        member.call("/membresias", "POST", {"socioId": socio["id"], "nivelMembresiaId": level["id"], "medioPago": "EFECTIVO"}, 201)
        socio = member.call("/socios/me")
    if socio["estadoMembresia"] == "PENDIENTE_PAGO":
        pay = member.call(f"/pagos/membresias/{socio['membresiaId']}", "POST", {
            "medioPago": "EFECTIVO", "claveSolicitud": str(uuid.uuid5(uuid.NAMESPACE_URL, "sera-demo-cuota"))})
        admin.call(f"/pagos/{pay['id']}/confirmacion-efectivo", "POST")
    today = datetime.now(timezone(timedelta(hours=-3))).date()
    space = next(s for s in admin.call("/espacios") if s["nombre"] == "Cancha cubierta")
    existing = member.call("/reservas")
    if not any(r.get("consumidaEn") for r in existing):
        reservation = member.call("/reservas", "POST", {"espacioId": space["id"],
            "fecha": str(today + timedelta(days=1)), "desde": "10:00", "hasta": "11:00",
            "personas": 2, "medioPago": "EFECTIVO", "claveSolicitud": str(uuid.uuid5(uuid.NAMESPACE_URL, "sera-demo-reserva-utilizada"))}, 201)
        admin.call(f"/pagos/{reservation['pagoId']}/confirmacion-efectivo", "POST")
        # La API permite reservar solo a futuro. El fixture representa una utilización
        # de ayer; se ajusta exclusivamente esta reserva ficticia dentro de Docker.
        rid = str(uuid.UUID(reservation["id"]))
        aid = str(uuid.UUID(admin.call("/usuarios/me")["id"]))
        yesterday = today - timedelta(days=1)
        sql = f"UPDATE reservas SET fecha='{yesterday}', consumida_en='{yesterday} 10:00:00-03', consumida_por='{aid}' WHERE id='{rid}';"
        subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", database, "-v", "ON_ERROR_STOP=1", "-c", sql], cwd=ROOT, check=True, capture_output=True)
    if not admin.call("/encuestas"):
        admin.call("/encuestas", "POST", {"titulo": "Tu experiencia en el polideportivo", "descripcion": "Datos de demostración. Contanos cómo fue tu visita.", "espacioId": space["id"], "desde": str(today), "hasta": str(today + timedelta(days=30)), "preguntas": [
            {"texto": "¿Cómo calificás el espacio?", "tipo": "CALIFICACION", "obligatoria": True, "opciones": []},
            {"texto": "¿Volverías a reservar?", "tipo": "OPCION", "obligatoria": True, "opciones": ["Sí", "No"]},
            {"texto": "¿Qué mejorarías?", "tipo": "TEXTO", "obligatoria": False, "opciones": []}]}, 201)
    if not admin.call("/reportes"):
        for kind in ["socios", "reservas", "pagos", "uso_servicios"]:
            admin.call("/reportes", "POST", {"tipo": kind, "desde": str(today - timedelta(days=30)), "hasta": str(today + timedelta(days=30))}, 201)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", default="sera")
    args = parser.parse_args()
    password = os.environ.get("SERA_DEMO_PASSWORD") or getpass.getpass("Contraseña para las dos cuentas ficticias, mínimo 8 caracteres: ")
    if len(password) < 8 or len(password.encode()) > 72:
        raise ValueError("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes.")
    seed(args.database, password)
