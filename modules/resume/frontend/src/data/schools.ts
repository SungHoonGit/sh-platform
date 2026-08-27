export interface SchoolOption {
  name: string;
  type: "고등학교" | "대학교" | "대학원";
}

export const SCHOOLS: SchoolOption[] = [
  { name: "서울대학교", type: "대학교" },
  { name: "연세대학교", type: "대학교" },
  { name: "고려대학교", type: "대학교" },
  { name: "서강대학교", type: "대학교" },
  { name: "성균관대학교", type: "대학교" },
  { name: "한양대학교", type: "대학교" },
  { name: "중앙대학교", type: "대학교" },
  { name: "경희대학교", type: "대학교" },
  { name: "한국외국어대학교", type: "대학교" },
  { name: "서울시립대학교", type: "대학교" },
  { name: "건국대학교", type: "대학교" },
  { name: "동국대학교", type: "대학교" },
  { name: "홍익대학교", type: "대학교" },
  { name: "숭실대학교", type: "대학교" },
  { name: "국민대학교", type: "대학교" },
  { name: "세종대학교", type: "대학교" },
  { name: "이화여자대학교", type: "대학교" },
  { name: "숙명여자대학교", type: "대학교" },
  { name: "한국기술교육대학교", type: "대학교" },
  { name: "인하대학교", type: "대학교" },
  { name: "아주대학교", type: "대학교" },
  { name: "한양대학교 ERICA", type: "대학교" },
  { name: "인천대학교", type: "대학교" },
  { name: "가천대학교", type: "대학교" },
  { name: "단국대학교", type: "대학교" },
  { name: "경기대학교", type: "대학교" },
  { name: "부산대학교", type: "대학교" },
  { name: "경북대학교", type: "대학교" },
  { name: "전남대학교", type: "대학교" },
  { name: "전북대학교", type: "대학교" },
  { name: "충남대학교", type: "대학교" },
  { name: "충북대학교", type: "대학교" },
  { name: "강원대학교", type: "대학교" },
  { name: "제주대학교", type: "대학교" },
  { name: "목포대학교", type: "대학교" },
  { name: "동아대학교", type: "대학교" },
  { name: "부경대학교", type: "대학교" },
  { name: "울산대학교", type: "대학교" },
  { name: "서울사이버대학교", type: "대학교" },
  { name: "고려사이버대학교", type: "대학교" },
  { name: "한국방송통신대학교", type: "대학교" },
  { name: "세종사이버대학교", type: "대학교" },
  { name: "서울대학교 대학원", type: "대학원" },
  { name: "연세대학교 대학원", type: "대학원" },
  { name: "고려대학교 대학원", type: "대학원" },
  { name: "서강대학교 대학원", type: "대학원" },
  { name: "성균관대학교 대학원", type: "대학원" },
  { name: "한양대학교 대학원", type: "대학원" },
  { name: "KAIST 대학원", type: "대학원" },
  { name: "서울과학기술대학교 대학원", type: "대학원" },
];

export function pickSchoolType(name: string): string {
  const hit = SCHOOLS.find((s) => s.name === name);
  return hit ? hit.type : "대학교";
}
