import { type LucideIcon } from "lucide-react";
import { T } from "./tokens";

/**
 * 헤더 아래 2차 메뉴 바(컨텍스트 서브내비). 앱별로 items를 주입한다.
 * SHELL_VERSION: 1
 */
export default function SubNav({
  items,
}: {
  items: { label: string; href?: string; icon?: LucideIcon; active?: boolean; onClick?: () => void }[];
}) {
  return (
    <nav className={T.subnavBg}>
      {items.map((item) => {
        const Icon = item.icon;
        const cls = item.active ? T.subnavItemActive : T.subnavItemIdle;
        return item.href && !item.onClick ? (
          <a key={item.label} href={item.href} className={`${cls} flex items-center gap-1.5`}>
            {Icon && <Icon size={14} />}
            {item.label}
          </a>
        ) : (
          <button
            key={item.label}
            onClick={item.onClick}
            className={`${cls} flex items-center gap-1.5 cursor-pointer`}
          >
            {Icon && <Icon size={14} />}
            {item.label}
          </button>
        );
      })}
    </nav>
  );
}
