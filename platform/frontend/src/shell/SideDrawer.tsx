import { useEffect, useState } from "react";
import { ChevronDown, ChevronRight, ExternalLink } from "lucide-react";
import { T } from "./tokens";

/**
 * 계층형(2단계 아코디언) 사이드 드로어.
 * - 그룹(섹션) 클릭 시 펼침/접힘, 현재 라우트가 속한 그룹은 자동 펼침
 * - 최대 2단계 고정 (그룹 → 항목)
 * SHELL_VERSION: 1
 */
export interface DrawerItem {
  label: string;
  href?: string;
  onClick?: () => void;
  external?: boolean;
}
export interface DrawerSection {
  label: string;
  items: DrawerItem[];
}

export default function SideDrawer({
  open,
  onClose,
  sections,
}: {
  open: boolean;
  onClose: () => void;
  sections: DrawerSection[];
}) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  // 현재 라우트가 속한 섹션 자동 펼침 (드로어 열릴 때마다 갱신)
  useEffect(() => {
    if (!open) return;
    const path = window.location.pathname + window.location.hash;
    const active = sections.find((s) => s.items.some((i) => i.href && path.startsWith(i.href)));
    if (active) setExpanded((prev) => new Set(prev).add(active.label));
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose, sections]);

  if (!open) return null;

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-30" onClick={onClose} />
      <aside className={`fixed left-0 top-0 bottom-0 ${T.drawerBg} z-40 flex flex-col shadow-2xl animate-[slideIn_.15s_ease-out]`}>
        <style>{`@keyframes slideIn{from{transform:translateX(-100%)}to{transform:translateX(0)}}`}</style>
        <div className="px-4 py-3.5 border-b border-slate-700 flex items-center justify-between shrink-0">
          <span className="text-sm font-bold text-white">메뉴</span>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-xl leading-none px-1" aria-label="닫기">
            ×
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto py-2">
          {sections.map((section) => {
            const isOpen = expanded.has(section.label);
            return (
              <div key={section.label}>
                <button
                  onClick={() =>
                    setExpanded((prev) => {
                      const next = new Set(prev);
                      if (next.has(section.label)) next.delete(section.label);
                      else next.add(section.label);
                      return next;
                    })
                  }
                  aria-expanded={isOpen}
                  className={T.drawerSectionHeader}
                >
                  <span>{section.label}</span>
                  {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                </button>
                {isOpen && (
                  <div className="pb-1">
                    {section.items.map((item) => {
                      const cls = `${T.drawerItem}`;
                      return item.href && !item.onClick ? (
                        <a
                          key={item.label}
                          href={item.href}
                          target={item.external ? "_blank" : undefined}
                          rel={item.external ? "noopener noreferrer" : undefined}
                          className={`${cls} flex items-center gap-2`}
                          onClick={onClose}
                        >
                          {item.label}
                          {item.external && <ExternalLink size={12} className="text-slate-500" />}
                        </a>
                      ) : (
                        <button
                          key={item.label}
                          onClick={() => {
                            item.onClick?.();
                            onClose();
                          }}
                          className={`${cls} flex items-center gap-2`}
                        >
                          {item.label}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>
      </aside>
    </>
  );
}
