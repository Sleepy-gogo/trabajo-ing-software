import {
  CalendarClock,
  CircleCheck,
  Clock,
  CircleX,
  TimerOff,
  Wallet,
} from "lucide-react"
import { reservationState, type Reservation } from "@/lib/reservations-api"
const states = {
  CONFIRMADA: {
    icon: CalendarClock,
    style: "bg-blue-50 text-blue-800 border-blue-200",
  },
  EN_CURSO: {
    icon: Clock,
    style: "bg-emerald-50 text-emerald-900 border-emerald-300",
  },
  CONSUMIDA: {
    icon: CircleCheck,
    style: "bg-violet-50 text-violet-900 border-violet-200",
  },
  FINALIZADA: {
    icon: TimerOff,
    style: "bg-slate-100 text-slate-700 border-slate-200",
  },
  CANCELADA: { icon: CircleX, style: "bg-red-50 text-red-800 border-red-200" },
  VENCIDA: {
    icon: TimerOff,
    style: "bg-slate-100 text-slate-700 border-slate-200",
  },
  PENDIENTE_PAGO: {
    icon: Wallet,
    style: "bg-amber-50 text-amber-900 border-amber-200",
  },
}
export function ReservationStatus({ state }: { state: Reservation["estado"] }) {
  const { icon: Icon, style } = states[state]
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold whitespace-nowrap ${style}`}
    >
      <Icon aria-hidden="true" className="size-3.5" />
      {reservationState[state]}
    </span>
  )
}
