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
