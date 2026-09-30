"""Prueba HTTP de membresías en sera_membresias_test, API 4501 sin credenciales MP."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path
import json
import subprocess
import uuid

from demo import Client, ROOT

BASE = "http://127.0.0.1:4501"
DATABASE = "sera_membresias_test"
PASSWORD = "MembresiasTest2026!"


def sql(statement):
    result = subprocess.run([
        "docker", "compose", "exec", "-T", "postgres", "psql", "-U", "sera", "-d", DATABASE,
        "-v", "ON_ERROR_STOP=1", "-At", "-c", statement,
    ], cwd=ROOT, check=True, capture_output=True, text=True)
    return result.stdout.strip()


def main():
    suffix = uuid.uuid4().hex[:8]
    actors = []
    for index in range(3):
        client = Client(BASE)
        email = f"membresia-{suffix}-{index}@example.com"
        user = client.call("/auth/registro", "POST", {
            "nombreCompleto": f"Membresía {suffix} {index}", "email": email,
            "dni": 10000000 + int(uuid.uuid4().hex[:6], 16), "password": PASSWORD,
            "relacionUnse": "EXTERNO",
        }, 201)
        uid = str(uuid.UUID(user["id"]))
        assert sql(f"SELECT count(*) FROM usuarios WHERE id='{uid}';") == "1", "La API no usa la base de pruebas"
        actors.append((client, email, uid))
    admin, admin_email, admin_id = actors[0]
    owner, owner_email, _ = actors[1]
    stranger = actors[2][0]
    sql(f"UPDATE usuarios SET rol='ADMIN' WHERE id='{admin_id}';")
    admin.login(admin_email, PASSWORD)
    owner.login(owner_email, PASSWORD)
    stranger.login(actors[2][1], PASSWORD)
    socio = owner.call("/socios/me")
    level = admin.call("/niveles-membresia", "POST", {
        "nombre": f"Membresía {suffix}", "descripcion": "Nivel para probar contratación y renovación",
        "preciosPorRelacion": {"EXTERNO": 1000}, "beneficios": ["Acceso de prueba"],
        "disponibleParaContratar": True,
    }, 201)

    def contract(method):
        return owner.call("/membresias", "POST", {
            "socioId": socio["id"], "nivelMembresiaId": level["id"], "medioPago": method,
        }, 201)

    membership = contract("MERCADO_PAGO")
    mid = membership["id"]
    billing_path = f"/pagos/membresias/{mid}"
    initial = owner.call(billing_path)["pagoPendiente"]
    assert membership["estado"] == "PENDIENTE_PAGO" and initial["medioPago"] == "MERCADO_PAGO"
    stranger.call(billing_path, expected=403)
    stranger.call(f"/membresias/{mid}/cancelacion", "POST", {"motivo": "Ajeno"}, 403)
    owner.call(billing_path + "/suscripcion", "POST", expected=409)
    owner.call(billing_path + "/verificacion", "POST")
    assert owner.call("/socios/me")["estadoMembresia"] == "PENDIENTE_PAGO"
    owner.call(f"/membresias/{mid}/cancelacion", "POST", {"motivo": "Cambiar a efectivo"})
    owner.call(f"/membresias/{mid}/cancelacion", "POST", {"motivo": "Reintento"})
    assert owner.call(f"/pagos/{initial['id']}")["estado"] == "CANCELADO"
    assert owner.call(billing_path)["pagoPendiente"] is None

    cash = contract("EFECTIVO")
    assert cash["id"] == mid and cash["proximoVencimiento"] is None
    payment = owner.call(billing_path)["pagoPendiente"]
    key_old = sql(f"SELECT clave_solicitud FROM pagos WHERE id='{payment['id']}';")
    owner.call(f"/pagos/{payment['id']}/confirmacion-efectivo", "POST", expected=403)
    another_admin = Client(BASE)
    another_admin.login(admin_email, PASSWORD)

    def confirm(client):
        try:
            return client.call(f"/pagos/{payment['id']}/confirmacion-efectivo", "POST")
        except RuntimeError as error:
            assert "recibido 409" in str(error)
            return None

    with ThreadPoolExecutor(max_workers=2) as pool:
        confirmations = list(pool.map(confirm, [admin, another_admin]))
    assert any(confirmations)
    approved = owner.call(f"/pagos/{payment['id']}")
    expiration = owner.call("/socios/me")["proximoVencimiento"]
    assert approved["estado"] == "APROBADO" and approved["vencimientoResultante"] == expiration
    admin.call(f"/pagos/{payment['id']}/confirmacion-efectivo", "POST")
    assert owner.call("/socios/me")["proximoVencimiento"] == expiration
    owner.call(f"/membresias/{mid}/cancelacion", "POST", {"motivo": "Volver a contratar"})
    assert owner.call(f"/pagos/{payment['id']}/comprobante")["estado"] == "APROBADO"

    again = contract("MERCADO_PAGO")
    assert again["id"] == mid and again["proximoVencimiento"] is None
    current = owner.call(billing_path)["pagoPendiente"]
    assert current["id"] != initial["id"] and current["id"] != payment["id"]
    owner.call(billing_path, "POST", {"medioPago": "EFECTIVO", "claveSolicitud": key_old}, 409)
    # Una suscripción anterior cancelada no pertenece a esta contratación.
    sql(f"INSERT INTO suscripciones_mercado_pago (id,membresia_id,pago_inicial_id,estado,monto,created_at,updated_at) VALUES (gen_random_uuid(),'{mid}','{initial['id']}','canceled',1000,now(),now());")
    assert owner.call(billing_path)["suscripcion"] is None
    owner.call(billing_path + "/suscripcion", expected=400)
    owner.call(f"/membresias/{mid}/cancelacion", "POST", {"motivo": "Solicitar efectivo"})
    contract("EFECTIVO")
    current = owner.call(billing_path)["pagoPendiente"]

    # El pendiente puede quedar fuera de las 20 filas de la primera página.
    sql(f"""INSERT INTO pagos (id,concepto,usuario_id,estado,medio_pago,monto,membresia_id,
        created_at,updated_at,contratacion_id,clave_solicitud,relacion_aplicada,nivel_nombre_aplicado,requiere_revision,version)
        SELECT gen_random_uuid(),concepto,usuario_id,'CANCELADO',medio_pago,monto,membresia_id,
        now() + i * interval '1 second',now(),contratacion_id,gen_random_uuid(),relacion_aplicada,nivel_nombre_aplicado,false,0
        FROM pagos CROSS JOIN generate_series(1,21) i WHERE id='{initial['id']}';""")
    assert all(p["id"] != current["id"] for p in owner.call("/pagos")["content"])
    assert owner.call(billing_path)["pagoPendiente"]["id"] == current["id"]
    admin.call(f"/pagos/{current['id']}/confirmacion-efectivo", "POST")
    yesterday = datetime.now(timezone(timedelta(hours=-3))).date() - timedelta(days=1)
    sql(f"UPDATE membresias SET proximo_vencimiento='{yesterday}',estado='ACTIVA' WHERE id='{mid}';")
    assert owner.call("/socios/me")["estadoMembresia"] == "VENCIDA"
    renewal = owner.call(billing_path, "POST", {"medioPago": "EFECTIVO", "claveSolicitud": str(uuid.uuid4())})
    renewed = admin.call(f"/pagos/{renewal['id']}/confirmacion-efectivo", "POST")
    assert owner.call("/socios/me")["estadoMembresia"] == "ACTIVA"
    assert renewed["vencimientoResultante"] > str(yesterday)
    assert sql(f"SELECT count(*) FROM membresias WHERE socio_id='{socio['id']}';") == "1"
    fixture = {"admin": admin_email, "socio": owner_email, "membresiaId": mid, "nivel": level["nombre"]}
    directory = ROOT / ".sera" / "logs"
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "membership-fixture.json").write_text(json.dumps(fixture), encoding="utf-8")
    print(json.dumps({"resultado": "OK", "casos": "contratación, checkout no disponible, permisos, confirmación concurrente única, cancelación, cambio de medio, nueva contratación, historial paginado y renovación vencida", **fixture}, ensure_ascii=False))


if __name__ == "__main__":
    main()
