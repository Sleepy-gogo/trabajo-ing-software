import { describe, expect, it, vi } from "vitest"
import { availableStarts } from "@/lib/booking-times"
import { createRequestKey } from "@/lib/request-key"

describe("Horarios de reserva", () => {
  it("redondea los segundos sin ofrecer un inicio pasado", () => {
    expect(
      availableStarts([{ desde: "10:30:01", hasta: "12:00:00" }], 1)
    ).toEqual(["11:00"])
  })
  it("no cruza bloqueos ni ofrece duraciones que no entran", () => {
    expect(
      availableStarts(
        [
          { desde: "08:00:00", hasta: "09:00:00" },
          { desde: "10:00:00", hasta: "12:00:00" },
        ],
        2
      )
    ).toEqual(["10:00"])
    expect(
      availableStarts([{ desde: "23:30:00", hasta: "23:59:00" }], 1)
    ).toEqual([])
  })
})

it("genera claves UUID v4 distintas en HTTP sin randomUUID", () => {
  const getRandomValues = crypto.getRandomValues.bind(crypto)
  vi.stubGlobal("crypto", { getRandomValues })
  const keys = Array.from({ length: 100 }, createRequestKey)
  expect(new Set(keys).size).toBe(keys.length)
  for (const key of keys)
    expect(key).toMatch(
      /^[\da-f]{8}-[\da-f]{4}-4[\da-f]{3}-[89ab][\da-f]{3}-[\da-f]{12}$/
    )
})
