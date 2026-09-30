"""Prueba HTTP de reportes y encuestas en sera_cierre_test, API 4501."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import json
import subprocess
import uuid

from demo import Client, ROOT

BASE = "http://127.0.0.1:4501"
DATABASE = "sera_cierre_test"
PASSWORD = "CierreTest2026!"


def sql(statement):
    result = subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql",
        "-U", "sera", "-d", DATABASE, "-v", "ON_ERROR_STOP=1", "-At", "-c", statement],
        cwd=ROOT, check=True, capture_output=True, text=True)
    return result.stdout.strip()


def main():
    today = datetime.now(timezone(timedelta(hours=-3))).date()
    suffix = uuid.uuid4().hex[:8]
    actors = []
    for index in range(3):
        client = Client(BASE)
        email = f"cierre-{suffix}-{index}@example.com"
        user = client.call("/auth/registro", "POST", {"nombreCompleto": f"Cierre {suffix} {index}",
            "email": email, "dni": 10000000 + int(uuid.uuid4().hex[:6], 16), "password": PASSWORD,
            "relacionUnse": "EXTERNO"}, 201)
        assert sql(f"SELECT count(*) FROM usuarios WHERE id='{uuid.UUID(user['id'])}'") == "1", "La API no usa sera_cierre_test"
        actors.append((client, email, user["id"]))
    admin, admin_email, admin_id = actors[0]
    owner, owner_email, _ = actors[1]
    stranger = actors[2][0]
    sql(f"UPDATE usuarios SET rol='ADMIN' WHERE id='{uuid.UUID(admin_id)}'")
    for client, email, _ in actors:
        client.login(email, PASSWORD)
    level = admin.call("/niveles-membresia", "POST", {"nombre": f"Nivel {suffix}", "descripcion": "Prueba",
        "beneficios": ["Prueba de membresía"], "disponibleParaContratar": True, "preciosPorRelacion": {"EXTERNO": 1000}}, 201)
    member = owner.call("/socios/me")
    membership = owner.call("/membresias", "POST", {"socioId": member["id"], "nivelMembresiaId": level["id"], "medioPago": "EFECTIVO"}, 201)
    pay = owner.call(f"/pagos/membresias/{membership['id']}", "POST", {"medioPago": "EFECTIVO", "claveSolicitud": str(uuid.uuid4())})
    owner.call(f"/pagos/{pay['id']}/confirmacion-efectivo", "POST", expected=403)
    admin.call(f"/pagos/{pay['id']}/confirmacion-efectivo", "POST")
    due = owner.call("/socios/me")["proximoVencimiento"]
    admin.call(f"/pagos/{pay['id']}/confirmacion-efectivo", "POST")
    assert owner.call("/socios/me")["proximoVencimiento"] == due
    assert owner.call("/socios/me")["estadoMembresia"] == "ACTIVA"
    stranger.call(f"/pagos/{pay['id']}", expected=403)
    space = admin.call("/espacios", "POST", {"nombre": f"Espacio {suffix}", "capacidad": 10,
        "tarifaHora": 100, "tipo": "Cancha"}, 201)
    tomorrow = today + timedelta(days=1)
    weekdays = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"]
    admin.call(f"/espacios/{space['id']}/disponibilidades", "POST", {"diaSemana": weekdays[tomorrow.weekday()], "horaDesde": "08:00", "horaHasta": "22:00"}, 201)
    r = owner.call("/reservas", "POST", {"espacioId": space["id"], "fecha": str(tomorrow),
        "desde": "10:00", "hasta": "12:00", "personas": 3, "medioPago": "EFECTIVO", "claveSolicitud": str(uuid.uuid4())}, 201)
    admin.call(f"/pagos/{r['pagoId']}/confirmacion-efectivo", "POST")
    # Solo este fixture se convierte en una utilización histórica.
    sql(f"UPDATE reservas SET fecha='{today}', consumida_en=now(), consumida_por='{uuid.UUID(admin_id)}' WHERE id='{uuid.UUID(r['id'])}'")
    survey_data = {"titulo": f"Encuesta {suffix}", "descripcion": "Prueba de integración", "espacioId": space["id"],
        "desde": str(today), "hasta": str(today + timedelta(days=1)), "preguntas": [
            {"texto": "Calificación", "tipo": "CALIFICACION", "obligatoria": True, "opciones": []},
            {"texto": "Volverías", "tipo": "OPCION", "obligatoria": True, "opciones": ["Sí", "No"]},
            {"texto": "Comentario", "tipo": "TEXTO", "obligatoria": False, "opciones": []}]}
    owner.call("/encuestas", "POST", survey_data, 403)
    survey = admin.call("/encuestas", "POST", survey_data, 201)
    sid = survey["id"]
    path = f"/encuestas/{sid}/reservas/{r['id']}"
    stranger.call(path, expected=403)
    assert any(item["encuesta"]["id"] == sid for item in owner.call("/encuestas/me"))
    owner.call(path + "/respuestas", "POST", {"respuestas": {}}, 400)
    answers = {survey["preguntas"][0]["id"]: "4", survey["preguntas"][1]["id"]: "Sí",
               survey["preguntas"][2]["id"]: 'Texto con coma, comillas " y\nsalto'}
    owner.call(path + "/respuestas", "POST", {"respuestas": {**answers, survey["preguntas"][0]["id"]: "6"}}, 400)
    owner.call(path + "/respuestas", "POST", {"respuestas": {**answers, str(uuid.uuid4()): "4"}}, 400)
    admin.call(f"/encuestas/{sid}/estado", "PUT", {"activa": False})
    owner.call(path + "/respuestas", "POST", {"respuestas": answers}, 409)
    admin.call(f"/encuestas/{sid}/estado", "PUT", {"activa": True})
    def submit(_):
        concurrent = Client(BASE)
        concurrent.login(owner_email, PASSWORD)
        try:
            return concurrent.call(path + "/respuestas", "POST", {"respuestas": answers}, 201)
        except RuntimeError as error:
            assert "recibido 409" in str(error), str(error)
            return None
    with ThreadPoolExecutor(max_workers=2) as pool:
        submitted = list(pool.map(submit, range(2)))
    assert sum(item is not None for item in submitted) == 1
    assert owner.call(path)["respuestas"] == answers
    owner.call(f"/encuestas/{sid}/resultados", expected=403)
    results = admin.call(f"/encuestas/{sid}/resultados")
    assert results["total"] == 1 and results["estadisticas"][0]["promedio"] == 4
    assert results["estadisticas"][1]["distribucion"] == {"Sí": 1}
    report_ids = []
    for kind in ["socios", "reservas", "pagos", "uso_servicios"]:
        filters = {"tipo": kind, "desde": str(today - timedelta(days=1)), "hasta": str(tomorrow),
                   "estado": "", "relacion": "EXTERNO", "espacioId": None if kind == "socios" else space["id"]}
        owner.call("/reportes", "POST", filters, 403)
        report = admin.call("/reportes", "POST", filters, 201)
        report_ids.append(report["id"])
        assert report["filas"], kind
        assert admin.call(f"/reportes/{report['id']}") == report
        owner.call(f"/reportes/{report['id']}", expected=403)
        with admin.opener.open(BASE + f"/api/reportes/{report['id']}/csv") as response:
            csv = response.read().decode("utf-8-sig")
            assert "text/csv" in response.headers["Content-Type"]
            assert "attachment" in response.headers["Content-Disposition"]
            assert report["columnas"][0]["label"] in csv
        if kind == "uso_servicios":
            row = report["filas"][0]
            assert row["reservas"] == 1 and row["ingresos"] == 1 and row["horas"] == 2 and row["personas"] == 3, row
        if kind == "pagos":
            assert report["resumen"]["importe_aprobado"] == 200
        if kind == "reservas":
            assert report["filas"][0]["estado"] == "CONSUMIDA"
    assert set(report_ids).issubset({item["id"] for item in admin.call("/reportes")})
    empty = admin.call("/reportes", "POST", {"tipo": "reservas", "desde": str(today - timedelta(days=30)), "hasta": str(today - timedelta(days=29)), "espacioId": space["id"]}, 201)
    assert empty["filas"] == []
    admin.call("/reportes", "POST", {"tipo": "pagos", "desde": str(tomorrow), "hasta": str(today)}, 400)
    admin.call("/reportes", "POST", {"tipo": "pagos", "desde": str(today), "hasta": str(today), "estado": "ACTIVA"}, 400)
    print(json.dumps({"resultado": "OK", "casos": "pago efectivo, renovación única, permisos, 4 reportes SQL, filtros, CSV, historial, preguntas, cierre, respuesta concurrente única y estadísticas"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
