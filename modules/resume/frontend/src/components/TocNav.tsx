import { useEffect, useMemo, useState } from "react";
import type { ResumeView } from "../types/resume";
import { SECTION_LABELS } from "./templates/shared";

/** 템플릿이 배열 데이터로 렌더링하는 섹션 키 (order 순서는 페이지가 전달) */
const ARRAY_SECTION_KEYS = new Set([
  "careers",
  "projects",
  "educations",
  "skills",
  "certificates",
  "introductions",
  "portfolioItems",
]);

/**
 * 이력서 뷰 우측 목차(TOC) 사이드바.
 * 데이터가 존재하는 섹션만 표시하고, 클릭 시 섹션으로 스크롤하며,
 * IntersectionObserver로 현재 스크롤 위치의 섹션을 하이라이트한다.
 * lg 미만 화면과 인쇄에서는 숨겨진다.
 */
export default function TocNav({ view, order }: { view: ResumeView; order: string[] }) {
  const [activeKey, setActiveKey] = useState<string | null>(null);

  const entries = useMemo(
    () =>
      order.filter((k) => {
        if (!ARRAY_SECTION_KEYS.has(k)) return false;
        const value = view[k as keyof ResumeView];
        return Array.isArray(value) && value.length > 0;
      }),
    [view, order],
  );

  const entryKey = entries.join(",");

  useEffect(() => {
    if (entries.length === 0) return;
    const els = entries
      .map((k) => document.getElementById(`section-${k}`))
      .filter((el): el is HTMLElement => el !== null);
    if (els.length === 0) return;

    const observer = new IntersectionObserver(
      (records) => {
        const hit = records
          .filter((r) => r.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
        if (hit) setActiveKey(hit.target.id.replace(/^section-/, ""));
      },
      { rootMargin: "-8% 0px -70% 0px", threshold: 0 },
    );
    els.forEach((el) => observer.observe(el));

    // 짧은 마지막 섹션이 관찰 밴드(상단 8~30%)에 닿지 않으면 활성값이 멈추므로
    // 문서 바닥 근처에서는 마지막 항목으로 보정한다.
    const onScroll = () => {
      const nearBottom =
        window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 8;
      if (nearBottom) setActiveKey(entries[entries.length - 1]);
    };
    window.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      observer.disconnect();
      window.removeEventListener("scroll", onScroll);
    };
  }, [entryKey, entries]);

  if (entries.length === 0) return null;
  const current = activeKey && entries.includes(activeKey) ? activeKey : entries[0];

  return (
    <nav aria-label="목차" className="hidden lg:block w-44 shrink-0 print:hidden">
      <div className="sticky top-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <p className="mb-2.5 text-[11px] font-bold uppercase tracking-wider text-slate-400">목차</p>
        <ul className="space-y-0.5 border-l border-slate-200">
          {entries.map((k) => {
            const active = current === k;
            return (
              <li key={k}>
                <button
                  type="button"
                  onClick={() => {
                    document
                      .getElementById(`section-${k}`)
                      ?.scrollIntoView({ behavior: "smooth", block: "start" });
                    setActiveKey(k);
                  }}
                  className={`-ml-px block w-full border-l-2 py-1 pl-3 text-left text-sm transition-colors ${
                    active
                      ? "border-teal-600 font-semibold text-slate-900"
                      : "border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-800"
                  }`}
                >
                  {SECTION_LABELS[k] ?? k}
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </nav>
  );
}
