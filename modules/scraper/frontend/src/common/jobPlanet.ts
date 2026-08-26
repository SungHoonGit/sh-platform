/**
 * 잡플래닛 검색용 회사명 정규화.
 * "(주)", "주식회사" 등 법인 형태가 포함되면 잡플래닛 검색이 실패하므로 제거한다.
 */
export function jobPlanetQuery(company: string | null | undefined): string {
  if (!company) return "";
  return company
    .replace(/\(주\)|㈜|\(株\)|주식회사/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * 마감일 문자열을 파싱해 남은 일수를 반환한다.
 * 지원 형식: "2026-08-30", "~08/30", "08/30(월)", "08월30일" 등. 상시/채용시까지는 null.
 */
export function daysUntilDeadline(deadline: string | null | undefined): number | null {
  if (!deadline) return null;
  const d = deadline.replace(/[^0-9~/월일\s-]/g, "");
  let m: RegExpMatchArray | null;
  if ((m = d.match(/(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/))) {
    return diffDays(+m[1], +m[2], +m[3]);
  }
  if ((m = d.match(/(\d{1,2})\s*[~/]\s*(\d{1,2})/))) {
    const now = new Date();
    return diffDays(now.getFullYear(), +m[1], +m[2]);
  }
  if (/상시|채용시|마감없음|수시/.test(deadline)) return null;
  return null;
}

function diffDays(y: number, mo: number, day: number): number | null {
  const target = new Date(y, mo - 1, day);
  if (Number.isNaN(target.getTime())) return null;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

/** 마감 배지용 라벨. 임박(D-3~D-0)만 반환, 그 외 null */
export function deadlineBadge(deadline: string | null | undefined): string | null {
  const n = daysUntilDeadline(deadline);
  if (n == null || n > 3) return null;
  if (n < 0) return "마감";
  return `D-${n}`;
}
