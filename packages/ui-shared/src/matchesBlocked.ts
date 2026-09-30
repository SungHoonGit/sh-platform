/**
 * 차단 키워드 매칭 헬퍼 (설계 036 — FE 판정 단일 소스).
 * 백엔드 CompanyBlacklistService.BlockMatcher.firstMatch와 동일 규칙:
 * exact = 정규화명 정확일치, contains = 정규화명 부분일치(포함), null = exact(레거시).
 */

export type MatchType = "exact" | "contains";

export interface BlockEntryLike {
  companyNameNormalized: string;
  matchType?: MatchType | null;
}

/**
 * (질의형) 정규화 회사명과 일치하는 첫 차단 항목을 반환한다.
 * 목록 순서 유지(등록 역순 = 최신 우선) — 백엔드 firstMatch와 동일.
 *
 * @param entries 차단 목록 (blItems 등)
 * @param normalized 정규화 회사명
 * @return 매칭 항목, 없으면 null
 */
export function findBlockEntry<T extends BlockEntryLike>(
  entries: readonly T[] | null | undefined,
  normalized: string
): T | null {
  if (!entries || !normalized) return null;
  for (const e of entries) {
    if (!e.companyNameNormalized) continue;
    const hit =
      e.matchType === "contains"
        ? normalized.includes(e.companyNameNormalized)
        : normalized === e.companyNameNormalized;
    if (hit) return e;
  }
  return null;
}

/**
 * (질의형) 정규화 회사명이 차단되었는지 여부.
 *
 * @param entries 차단 목록 (blItems 등)
 * @param normalized 정규화 회사명
 * @return 차단 여부
 */
export function matchesBlocked(
  entries: readonly BlockEntryLike[] | null | undefined,
  normalized: string
): boolean {
  return findBlockEntry(entries, normalized) != null;
}
