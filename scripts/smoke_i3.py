"""Verifica incrementos 1 a 3 contra la API local; deja datos ficticios identificados como Prueba I3."""
import datetime
import os
import secrets
from concurrent.futures import ThreadPoolExecutor
from demo import Client


def check(password):
    admin = Client()
    admin.login("admin@sera.local", password)
    suffix = secrets.token_hex(4)
    member_dni = secrets.randbelow(1000000)+96000000
    member = Client()
    member.call("/auth/registro", "POST", {"nombreCompleto": "Prueba I3 socio " + suffix,
        "email": f"socio-{suffix}@sera.local", "dni": member_dni, "password": password,
        "relacionUnse": "ESTUDIANTE"}, 201)
    account = member.login(f"socio-{suffix}@sera.local", password)
    socio = member.call("/socios/me")
    assert socio["usuarioId"] == account["id"] and socio["estadoVerificacionUnse"] == "PENDIENTE"
    assert socio["membresiaId"] is None
    suffix = secrets.token_hex(4)
    outsider = Client()
    outsider.call("/auth/registro", "POST", {"nombreCompleto": "Prueba I3 privacidad", "email": f"prueba-{suffix}@sera.local", "dni": secrets.randbelow(1000000)+97000000, "password": password}, 201)
    outsider.login(f"prueba-{suffix}@sera.local", password)
    assert outsider.call("/socios/me")["membresiaId"] is None
    outsider.call("/socios", expected=403)
    member.call("/niveles-membresia", "POST", {}, 403)
    level = admin.call("/niveles-membresia?soloDisponibles=false")[0]
    data = {"socioId": socio["id"], "nivelMembresiaId": level["id"]}
    membership = member.call("/membresias", "POST", data, 201)
    assert membership["estado"] == "PENDIENTE_PAGO" and membership["proximoVencimiento"] is None
    member.call("/membresias", "POST", data, 409)
    outsider.call(f"/membresias/{membership['id']}", expected=403)
    outsider.call(f"/membresias/{membership['id']}/cancelacion", "POST", {"motivo": "Ajeno"}, 403)
    update = {"relacionUnse": "ESTUDIANTE", "estadoVerificacionUnse": "VERIFICADA", "identificadorUnse": None, "nivelMembresiaId": level["id"], "estadoMembresia": "ACTIVA", "motivo": "Prueba I3 activación inválida"}
    admin.call(f"/socios/{socio['id']}", "PUT", update, 409)
    assert member.call("/socios/me")["estadoVerificacionUnse"] == "PENDIENTE", "La transacción inválida debe revertir la verificación"
    update["estadoMembresia"] = "PENDIENTE_PAGO"
    admin.call(f"/socios/{socio['id']}", "PUT", update)
    filtered = admin.call(f"/socios?buscar={member_dni}&estadoMembresia=PENDIENTE_PAGO&relacionUnse=ESTUDIANTE")
    assert len(filtered) == 1 and filtered[0]["id"] == socio["id"]
    assert not admin.call(f"/socios?buscar={member_dni}&estadoMembresia=ACTIVA&relacionUnse=EXTERNO")
    cancelled = member.call(f"/membresias/{membership['id']}/cancelacion", "POST", {"motivo": "Prueba I3 cancelación"})
    assert cancelled["estado"] == "CANCELADA" and cancelled["fechaBaja"]
    renewed = member.call("/membresias", "POST", data, 201)
    assert renewed["id"] == membership["id"] and renewed["estado"] == "PENDIENTE_PAGO"
    assert len(admin.call(f"/socios/{socio['id']}/historial")) >= 4
    # Verify optional membership on an admin-edited socio.
    other = outsider.call("/socios/me")
    admin.call(f"/socios/{other['id']}", "PUT", {"relacionUnse": "EXTERNO", "estadoVerificacionUnse": "VERIFICADA", "identificadorUnse": None, "nivelMembresiaId": None, "estadoMembresia": None, "motivo": "Prueba I3 sin membresía"})
    space = admin.call("/espacios", "POST", {"nombre": f"Prueba I3 {suffix}", "descripcion": "Espacio para validación automática", "capacidad": 10, "tarifaHora": 1200, "tipo": "Prueba", "rutaImagen": None}, 201)
    sid = space["id"]
    date = (datetime.date.today()+datetime.timedelta(days=2)).isoformat()
    day = ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES","SABADO","DOMINGO"][datetime.date.fromisoformat(date).weekday()]
    horario = {"diaSemana": day, "horaDesde": "08:00", "horaHasta": "18:00"}
    admin.call(f"/espacios/{sid}/disponibilidades", "POST", horario, 201)
    admin.call(f"/espacios/{sid}/disponibilidades", "POST", horario, 409)
    admin.call(f"/espacios/{sid}/tarifas", "PUT", {"tarifas": {"ESTUDIANTE": 600}})
    block = admin.call(f"/espacios/{sid}/bloqueos", "POST", {"fecha": date, "desde": "10:00", "hasta": "12:00", "motivo": "Prueba I3 mantenimiento"}, 201)
    result = member.call(f"/espacios/{sid}/calendario?fecha={date}")
    assert result["tarifaHora"] == 600 and result["relacionAplicada"] == "ESTUDIANTE"
    assert [(f["desde"][:5],f["hasta"][:5]) for f in result["franjas"]] == [("08:00","10:00"),("12:00","18:00")]
    outsider.call(f"/espacios/{sid}/bloqueos?fecha={date}", expected=403)
    member.call(f"/espacios/{sid}/estado", "PUT", {"estado": "MANTENIMIENTO"}, 403)
    admin.call(f"/espacios/{sid}/estado", "PUT", {"estado": "MANTENIMIENTO"})
    assert not member.call(f"/espacios/{sid}/calendario?fecha={date}")["franjas"]
    admin.call(f"/espacios/{sid}/estado", "PUT", {"estado": "HABILITADO"})
    admin.call(f"/espacios/{sid}/bloqueos/{block['id']}", "DELETE", expected=204)
    assert len(member.call(f"/espacios/{sid}/calendario?fecha={date}")["franjas"]) == 1
    # Independent sessions race for the same weekly range. The space row serializes writes.
    def race(_):
        c = Client(); c.login("admin@sera.local", password)
        try:
            c.call(f"/espacios/{sid}/disponibilidades", "POST", {"diaSemana": day,"horaDesde":"18:00","horaHasta":"20:00"},201)
            return 201
        except RuntimeError as error:
            if "recibido 409" in str(error): return 409
            raise
    with ThreadPoolExecutor(max_workers=2) as pool:
        assert sorted(pool.map(race,range(2))) == [201,409]
    admin.call(f"/espacios/{sid}", "DELETE", expected=204)
    assert admin.call(f"/espacios/{sid}")["estado"] == "INUTILIZABLE"
    print("OK: alta usuario/socio atómica, socio sin membresía, permisos, contratación, unicidad, rollback, filtros SQL, cancelación, recontratación, auditoría, tarifas, bloqueos, calendario y concurrencia.")

if __name__ == "__main__":
    check(os.environ["SERA_DEMO_PASSWORD"])
