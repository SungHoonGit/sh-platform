import { useEffect, useState } from "react";
import type { LucideIcon } from "lucide-react";
import { Search, CalendarPlus, FileText, Building2 } from "lucide-react";
import type { AppMenu, AppName, MenuItem } from "./menuTypes";

/** DB에 저장된 아이콘명 → lucide 컴포넌트 (메뉴에 사용되는 아이콘만) */
const ICONS: Record<string, LucideIcon> = { Search, CalendarPlus, FileText, Building2 };

type RawItem = Omit<MenuItem, "icon"> & { icon?: string };
type RawMenu = { app: string; items: RawItem[] };

const cache = new Map<string, Promise<RawMenu | null>>();

function fetchMenu(app: AppName): Promise<RawMenu | null> {
  let pending = cache.get(app);
  if (!pending) {
    pending = fetch(`/api/v1/menus?app=${app}`)
      .then((r) => (r.ok ? r.json() : null))
      .then((j) =>
        j && j.code === "SUCCESS" && Array.isArray(j.data?.items) && j.data.items.length > 0
          ? (j.data as RawMenu)
          : null,
      )
      .catch(() => null);
    cache.set(app, pending);
  }
  return pending;
}

/**
 * (조회형) 서버 메뉴(GET /api/v1/menus, DB 단일 소스)를 조회해 반환한다.
 * 실패·빈 값·미로그인 시 로컬 fallback(앱 menus.ts)을 그대로 사용한다.
 *
 * @param app      앱 코드
 * @param fallback 로컬 메뉴 (앱 src/menus.ts)
 * @return 화면에 사용할 AppMenu (최초 렌더는 fallback, 서버 메뉴 도착 후 갱신)
 */
export function useAppMenu(app: AppName, fallback: AppMenu): AppMenu {
  const [menu, setMenu] = useState<AppMenu>(fallback);
  useEffect(() => {
    let alive = true;
    void fetchMenu(app).then((remote) => {
      if (!alive || !remote) return;
      setMenu({
        app: remote.app as AppName,
        items: remote.items.map((i) => ({
          ...i,
          icon: i.icon != null ? (ICONS[i.icon] ?? undefined) : undefined,
        })),
      });
    });
    return () => {
      alive = false;
    };
  }, [app]);
  return menu;
}
