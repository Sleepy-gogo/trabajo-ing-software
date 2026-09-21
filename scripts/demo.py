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


def seed(database, password):
    if database not in ("sera", "sera_entrega_i3"):
        raise ValueError("La demo solo admite las bases locales sera y sera_entrega_i3.")
    client = Client()
    accounts = [("admin@sera.local", "Administración Demo", 99000001), ("socio@sera.local", "Estudiante Demo", 99000002)]
    for email, name, dni in accounts:
        try:
            client.login(email, password)
        except RuntimeError:
            client.call("/auth/registro", "POST", {"nombreCompleto": name, "email": email, "dni": dni, "password": password, "relacionUnse": "ESTUDIANTE"}, 201)
    # Bootstrap restricted to the fictitious administrator in local Docker.
    subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", database, "-v", "ON_ERROR_STOP=1", "-c", "UPDATE usuarios SET rol='ADMIN' WHERE email='admin@sera.local';"], cwd=ROOT, check=True, capture_output=True)
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
    print("Demo preparada: admin@sera.local y socio@sera.local. Usan la contraseña indicada.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", default="sera")
    args = parser.parse_args()
    password = os.environ.get("SERA_DEMO_PASSWORD") or getpass.getpass("Contraseña para las dos cuentas ficticias, mínimo 8 caracteres: ")
    if len(password) < 8 or len(password.encode()) > 72:
        raise ValueError("La contraseña debe tener al menos 8 caracteres y hasta 72 bytes.")
    seed(args.database, password)
