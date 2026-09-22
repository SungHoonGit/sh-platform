import { useEffect, useRef, useState } from "react";
import { CalendarDays, ChevronLeft, ChevronRight, X } from "lucide-react";

interface Props {
  /** YYYY-MM-DD (없으면 빈 문자열) */
  start: string;
  /** YYYY-MM-DD (없으면 진행 중) */
  end: string;
  onChange: (start: string, end: string) => void;
  disabled?: boolean;
}

const WEEK = ["일", "월", "화", "수", "목", "금", "토"];

function toISO(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

function parseISO(s: string): Date | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return null;
  const [y, m, d] = s.split("-").map(Number);
  return new Date(y, m - 1, d);
}

/**
 * 단일 달력에서 기간(시작~종료)을 클릭으로 선택하는 레인지 피커.
 * 시작일 클릭 → 종료일 클릭 순서로 선택하며, 종료일이 시작일보다 빠를 수 없다 (범위 체크).
 * 종료일 비움 = 진행 중.
 */
export default function DateRangePicker({ start, end, onChange, disabled }: Props) {
  const [open, setOpen] = useState(false);
  const [view, setView] = useState(() => parseISO(start) ?? new Date());
  const [picking, setPicking] = useState<"start" | "end">("start");
  const [draftStart, setDraftStart] = useState(start);
  const [draftEnd, setDraftEnd] = useState(end);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("mousedown", onDown);
    window.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("mousedown", onDown);
      window.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const openPicker = () => {
    if (disabled) return;
    setDraftStart(start);
    setDraftEnd(end);
    setPicking(start && end ? "start" : start ? "end" : "start");
    setView(parseISO(start) ?? new Date());
    setOpen(true);
  };

  const pick = (iso: string) => {
    if (picking === "start" || (draftStart && draftEnd)) {
      setDraftStart(iso);
      setDraftEnd("");
      setPicking("end");
      return;
    }
    // 범위 체크: 시작일보다 이전이면 새 시작일로 재시작
    if (iso < draftStart) {
      setDraftStart(iso);
      setDraftEnd("");
      setPicking("end");
      return;
    }
    setDraftEnd(iso);
    onChange(draftStart, iso);
    setOpen(false);
    setPicking("start");
  };

  const shiftMonth = (delta: number) => {
    setView((v) => new Date(v.getFullYear(), v.getMonth() + delta, 1));
  };

  const label = start
    ? `${start} ~ ${end || "진행 중"}`
    : end
      ? `~ ${end}`
      : "기간 선택";

  const year = view.getFullYear();
  const month = view.getMonth();
  const firstDow = new Date(year, month, 1).getDay();
  const days = new Date(year, month + 1, 0).getDate();
  const todayISO = toISO(new Date());
  // 달력 셀의 ISO (마감 범위 하이라이트용)
  const cellISO = (day: number) => `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        onClick={openPicker}
        disabled={disabled}
        className={`w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm text-left focus:outline-none focus:border-gray-500 ${
          disabled ? "bg-gray-100" : "bg-white hover:border-gray-400"
        }`}
      >
        <span className="inline-flex items-center gap-1.5 text-gray-700">
          <CalendarDays size={15} className="text-gray-400" />
          <span className={start ? "" : "text-gray-400"}>{label}</span>
        </span>
      </button>

      {open && (
        <div className="absolute z-30 mt-1 w-72 rounded-lg border border-gray-200 bg-white p-3 shadow-xl">
          <div className="mb-2 flex items-center justify-between">
            <button type="button" onClick={() => shiftMonth(-1)} className="rounded p-1 hover:bg-gray-100" title="이전 달">
              <ChevronLeft size={16} />
            </button>
            <span className="text-sm font-semibold text-gray-800">
              {year}년 {month + 1}월
            </span>
            <div className="flex items-center gap-1">
              <button type="button" onClick={() => shiftMonth(1)} className="rounded p-1 hover:bg-gray-100" title="다음 달">
                <ChevronRight size={16} />
              </button>
              <button type="button" onClick={() => setOpen(false)} className="rounded p-1 hover:bg-gray-100" title="닫기">
                <X size={14} />
              </button>
            </div>
          </div>

          <div className="mb-1 grid grid-cols-7 text-center text-[11px] font-medium text-gray-400">
            {WEEK.map((w) => (
              <div key={w} className="py-0.5">
                {w}
              </div>
            ))}
          </div>
          <div className="grid grid-cols-7 gap-y-0.5">
            {Array.from({ length: firstDow }).map((_, i) => (
              <div key={`pad-${i}`} />
            ))}
            {Array.from({ length: days }, (_, i) => i + 1).map((d) => {
              const iso = cellISO(d);
              const isStart = iso === draftStart;
              const isEnd = draftEnd && iso === draftEnd;
              const inRange = draftStart && draftEnd && iso > draftStart && iso < draftEnd;
              return (
                <button
                  key={iso}
                  type="button"
                  onClick={() => pick(iso)}
                  className={`mx-auto h-7 w-7 rounded-full text-xs transition-colors ${
                    isStart || isEnd
                      ? "bg-blue-600 font-semibold text-white"
                      : inRange
                        ? "bg-blue-50 text-blue-700"
                        : iso === todayISO
                          ? "text-blue-600 font-medium hover:bg-gray-100"
                          : "text-gray-700 hover:bg-gray-100"
                  }`}
                  title={iso}
                >
                  {d}
                </button>
              );
            })}
          </div>

          <div className="mt-2 flex items-center justify-between border-t border-gray-100 pt-2">
            <span className="text-[11px] text-gray-500">
              {picking === "end" && draftStart ? `${draftStart}의 종료일 선택…` : "시작일을 선택하세요"}
            </span>
            <div className="flex gap-1.5">
              {(draftStart || draftEnd) && (
                <button
                  type="button"
                  onClick={() => {
                    setDraftStart("");
                    setDraftEnd("");
                    onChange("", "");
                    setOpen(false);
                    setPicking("start");
                  }}
                  className="rounded border border-gray-200 px-2 py-1 text-xs text-gray-500 hover:bg-gray-50"
                >
                  초기화
                </button>
              )}
              <button
                type="button"
                onClick={() => {
                  setDraftEnd("");
                  onChange(draftStart, "");
                  setOpen(false);
                  setPicking("start");
                }}
                disabled={!draftStart}
                className="rounded border border-gray-200 px-2 py-1 text-xs text-gray-600 hover:bg-gray-50 disabled:opacity-40"
                title="종료일 없음(진행 중)"
              >
                진행 중
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
