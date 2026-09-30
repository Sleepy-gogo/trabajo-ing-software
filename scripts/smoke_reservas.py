"""Prueba HTTP del incremento 5 contra la base local aislada sera_reservas_test.

Iniciar la API con Docker Compose support desactivado y esa base. Crea datos
ficticios con un sufijo único; no borra registros. Mercado Pago se prueba con
mocks en ReservaServiceTest, sin cobros externos.
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta
from pathlib import Path
import json
import subprocess
import uuid

from demo import Client

ROOT = Path(__file__).resolve().parents[1]
DATABASE = "sera_reservas_test"
PASSWORD = "ReservaDemo2026!"


def sql(statement):
    result = subprocess.run(
        ["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera",
         "-d", DATABASE, "-v", "ON_ERROR_STOP=1", "-At", "-c", statement],
        cwd=ROOT, capture_output=True, text=True, check=True)
    return result.stdout.strip()


def main():
    suffix = uuid.uuid4().hex[:8]
    actors = []
    for i in range(3):
        client = Client("http://localhost:4501")
        email = f"reserva-{suffix}-{i}@example.com"
        user = client.call("/auth/registro", "POST", {
            "nombreCompleto": f"Reserva Demo {i}", "email": email,
            "dni": 10000000 + int(uuid.uuid4().hex[:6], 16),
            "password": PASSWORD, "relacionUnse": "EXTERNO"}, 201)
        actors.append((client, email, user["id"]))
    admin, email, admin_id = actors[0]
    # Confirm the API writes to the isolated database before promoting its test account.
    assert sql(f"SELECT count(*) FROM usuarios WHERE id='{admin_id}'") == "1", "La API no usa la base de pruebas"
    sql(f"UPDATE usuarios SET rol='ADMIN' WHERE id='{admin_id}'")
    for client, email, _ in actors:
        client.login(email, PASSWORD)
    one, two = actors[1][0], actors[2][0]
    space = admin.call("/espacios", "POST", {"nombre": f"Cancha {suffix}",
        "descripcion": "Prueba de reservas", "capacidad": 10, "tarifaHora": 1000,
        "tipo": "Cancha", "rutaImagen": None}, 201)
    sid = space["id"]
    day = date.today() + timedelta(days=2)
    weekday = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"][day.weekday()]
    admin.call(f"/espacios/{sid}/disponibilidades", "POST", {
        "diaSemana": weekday, "horaDesde": "08:00", "horaHasta": "22:00"}, 201)

    def data(start=10, end=11, ticket=None, method="EFECTIVO"):
        return {"espacioId": sid, "fecha": str(day), "desde": f"{start:02}:00",
                "hasta": f"{end:02}:00", "personas": 2, "ticketId": ticket,
                "medioPago": method, "claveSolicitud": str(uuid.uuid4())}

    request = data()
    quote = one.call("/reservas/cotizacion", "POST", request)
    assert quote["aPagar"] == 1000
    r = one.call("/reservas", "POST", request, 201)
    assert r["estado"] == "PENDIENTE_PAGO" and r["codigo"] is None
    assert one.call("/reservas", "POST", request, 201)["id"] == r["id"]
    two.call(f"/reservas/{r['id']}", expected=403)
    two.call("/reservas?todas=true", expected=403)
    two.call("/reservas", "POST", data(), 409)
    one.call(f"/pagos/{r['pagoId']}/confirmacion-efectivo", "POST", expected=403)
    admin.call(f"/espacios/{sid}/bloqueos", "POST", {
        "fecha": str(day), "desde": "10:30", "hasta": "11:30", "motivo": "Conflicto"}, 409)
    admin.call(f"/espacios/{sid}/estado", "PUT", {"estado": "MANTENIMIENTO"}, 409)
    admin.call(f"/pagos/{r['pagoId']}/confirmacion-efectivo", "POST")
    confirmed = one.call(f"/reservas/{r['id']}")
    assert confirmed["estado"] == "CONFIRMADA" and confirmed["codigo"]
    admin.call(f"/pagos/{r['pagoId']}/confirmacion-efectivo", "POST")
    assert one.call(f"/reservas/{r['id']}")["codigo"] == confirmed["codigo"]
    cancelled = one.call(f"/reservas/{r['id']}/cancelacion", "POST")
    assert cancelled["saldoTicket"] == 1000
    assert one.call(f"/reservas/{r['id']}/cancelacion", "POST")["saldoTicket"] == 1000

    # A ticket can cover part of the price; abandoning the pending reservation restores it.
    partial = one.call("/reservas", "POST", data(10, 12, r["id"]), 201)
    assert partial["creditoAplicado"] == 1000 and partial["total"] == 2000
    assert one.call(f"/pagos/{partial['pagoId']}")["conceptoPago"] == "DIFERENCIA_TICKET"
    one.call(f"/reservas/{partial['id']}/cancelacion", "POST")
    assert one.call(f"/reservas/{r['id']}")["saldoTicket"] == 1000
    two.call("/reservas", "POST", data(14, 15, r["id"]), 403)

    def attempt(client, body):
        try:
            return client.call("/reservas", "POST", body, 201)
        except RuntimeError as error:
            assert "recibido 409" in str(error), str(error)
            return None

    # Two different users contend for one slot: exactly one succeeds.
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda c: attempt(c, data(15, 16)), [one, two]))
    assert sum(v is not None for v in results) == 1

    # Two sessions for one owner contend for the same ticket at different times.
    another = Client("http://localhost:4501")
    another.login(actors[1][1], PASSWORD)
    with ThreadPoolExecutor(max_workers=2) as pool:
        futures = [pool.submit(attempt, one, data(17, 18, r["id"])),
                   pool.submit(attempt, another, data(18, 19, r["id"]))]
        results = [f.result() for f in futures]
    assert sum(v is not None for v in results) == 1
    winner = next(v for v in results if v)
    assert winner["estado"] == "CONFIRMADA" and winner["pagoId"] is None
    assert one.call(f"/reservas/{r['id']}")["saldoTicket"] == 0

    # Direct SQL also rejects overlap, proving the database constraint is active.
    try:
        sql(f"INSERT INTO reservas (id,usuario_id,espacio_id,fecha,desde,hasta,personas,tarifa_hora,relacion_aplicada,total,estado,codigo,vence_en,creada_en,ticket_origen_id,credito_aplicado,saldo_ticket,clave_solicitud,version) SELECT gen_random_uuid(),usuario_id,espacio_id,fecha,desde,hasta,personas,tarifa_hora,relacion_aplicada,total,estado,NULL,vence_en,creada_en,NULL,0,0,gen_random_uuid(),0 FROM reservas WHERE id='{winner['id']}'")
        raise AssertionError("PostgreSQL aceptó una superposición")
    except subprocess.CalledProcessError as error:
        assert "reservas_sin_superposicion" in error.stderr
    print(json.dumps({"resultado": "OK", "casos": "creación, precio, permisos, pago, cancelación, tickets, concurrencia y constraint PostgreSQL", "espacio": sid, "usuario": actors[1][1]}))


if __name__ == "__main__":
    main()
