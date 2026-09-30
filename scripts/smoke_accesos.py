"""Prueba de ingresos concurrentes en sera_reservas_test, API 4501."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import uuid

from demo import Client
from smoke_reservas import sql


def main():
    now = datetime.now(timezone(timedelta(hours=-3)))
    assert now.hour < 23, "Ejecutar antes de las 23 para no cruzar medianoche"
    suffix = uuid.uuid4().hex[:8]
    password = "AccesoTest2026!"
    email = f"acceso-{suffix}@example.com"
    operator = Client("http://localhost:4501")
    user = operator.call("/auth/registro", "POST", {
        "nombreCompleto": "Acceso Test", "email": email,
        "dni": 10000000 + int(uuid.uuid4().hex[:6], 16),
        "password": password, "relacionUnse": "EXTERNO"}, 201)
    uid = user["id"]
    assert sql(f"SELECT count(*) FROM usuarios WHERE id='{uid}'") == "1"
    sql(f"UPDATE usuarios SET rol='ADMIN' WHERE id='{uid}'")
    operator.login(email, password)
    space = operator.call("/espacios", "POST", {"nombre": f"Acceso {suffix}",
        "capacidad": 10, "tarifaHora": 1000, "tipo": "Cancha"}, 201)
    day = now.date() + timedelta(days=1)
    weekday = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"][day.weekday()]
    start, end = f"{now.hour:02}:00", f"{now.hour+1:02}:00"
    operator.call(f"/espacios/{space['id']}/disponibilidades", "POST", {
        "diaSemana": weekday, "horaDesde": start, "horaHasta": end}, 201)
    r = operator.call("/reservas", "POST", {"espacioId": space["id"], "fecha": str(day),
        "desde": start, "hasta": end, "personas": 1, "medioPago": "EFECTIVO",
        "claveSolicitud": str(uuid.uuid4())}, 201)
    operator.call(f"/pagos/{r['pagoId']}/confirmacion-efectivo", "POST")
    r = operator.call(f"/reservas/{r['id']}")
    code = {"codigo": r["codigo"]}
    assert not operator.call("/accesos/validacion", "POST", code)["autorizado"]
    # Ajustar solo esta reserva de prueba al horario actual.
    sql(f"UPDATE reservas SET fecha='{now.date()}' WHERE id='{r['id']}'")
    assert operator.call(f"/reservas/{r['id']}")["estado"] == "EN_CURSO"
    assert operator.call("/accesos/validacion", "POST", code)["autorizado"]
    def enter(_):
        c = Client("http://localhost:4501")
        c.login(email, password)
        return c.call("/accesos/ingresos", "POST", code)
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(enter, range(2)))
    assert datetime.fromisoformat(results[0]["consumidaEn"]) == datetime.fromisoformat(results[1]["consumidaEn"])
    assert results[0]["consumidaEn"] is not None
    assert not operator.call("/accesos/validacion", "POST", code)["autorizado"]
    assert operator.call(f"/reservas/{r['id']}")["estado"] == "CONSUMIDA"
    assert sql(f"SELECT consumida_por FROM reservas WHERE id='{r['id']}'") == uid
    print("OK: próxima, en curso, consumo concurrente único, operador y rechazo de segundo acceso")


if __name__ == "__main__":
    main()
