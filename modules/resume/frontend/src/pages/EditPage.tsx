import { useEffect, useMemo, useState } from "react";
import { apiGet, apiPut, logout } from "../api/client";
import type { ResumeView } from "../types/resume";
import type { ResumeDocument, SectionItem } from "../types/document";
import CrudSection, { type FieldDef } from "../components/CrudSection";
import ProfileEditor from "../components/ProfileEditor";
import {
  DEFAULT_ORDER,
  SECTION_LABELS,
  TEMPLATE_LABELS,
  TEMPLATE_OPTIONS,
} from "../components/templates/shared";

interface SectionConfig {
  key: string;
  title: string;
  endpoint: string;
  listKey: keyof ResumeView;
  titleKey: string;
  subtitleKeys: string[];
  fields: FieldDef[];
  fixedPayload?: Record<string, unknown>;
  /** inline 타입: 목록 행 자체가 편집 폼 (자기소개 등) */
  inline?: boolean;
}

const SECTIONS: SectionConfig[] = [
  {
    key: "careers",
    title: "경력",
    endpoint: "/careers",
    listKey: "careers",
    titleKey: "company",
    subtitleKeys: ["title", "description"],
    fields: [
      { key: "company", label: "회사명", required: true },
      { key: "title", label: "직무/직책" },
      { key: "startDate", label: "입사일", type: "date" },
      { key: "employed", label: "재직 중", type: "check", disablesOnCheck: ["endDate"] },
      { key: "endDate", label: "퇴사일", type: "date", placeholder: "재직 중이면 잠김" },
      { key: "description", label: "주요 업무", type: "textarea" },
    ],
  },
  {
    key: "projects",
    title: "프로젝트",
    endpoint: "/projects",
    listKey: "projects",
    titleKey: "name",
    subtitleKeys: ["role", "techStack"],
    fields: [
      { key: "name", label: "프로젝트명", required: true },
      { key: "role", label: "맡은 역할" },
      { key: "startDate", label: "시작일", type: "date" },
      { key: "endDate", label: "종료일", type: "date", placeholder: "진행 중이면 비움" },
      { key: "techStack", label: "기술 스택", placeholder: "쉼표로 구분: Java, Spring, MySQL" },
      { key: "linkUrl", label: "관련 링크 (GitHub 등)" },
      { key: "description", label: "프로젝트 설명", type: "textarea" },
    ],
  },
  {
    key: "educations",
    title: "학력",
    endpoint: "/educations",
    listKey: "educations",
    titleKey: "school",
    subtitleKeys: ["major"],
    fields: [
      { key: "school", label: "학교명", required: true, type: "school", placeholder: "학교명 입력 후 선택" },
      { key: "schoolType", label: "학교 유형", type: "select", options: ["고등학교", "대학교", "대학원"] },
      { key: "major", label: "전공", type: "major", placeholder: "전공명 입력 후 선택" },
      { key: "gpa", label: "학점", placeholder: "예: 3.9 / 4.5" },
      { key: "degree", label: "학위", placeholder: "예: 학사" },
      { key: "startDate", label: "입학일", type: "date" },
      { key: "endDate", label: "졸업일", type: "date" },
      { key: "status", label: "상태", type: "select", options: ["재학", "휴학", "졸업예정", "졸업", "수료"] },
    ],
  },
  {
    key: "skills",
    title: "스킬",
    endpoint: "/skills",
    listKey: "skills",
    titleKey: "name",
    subtitleKeys: ["level"],
    fields: [
      { key: "name", label: "스킬명", required: true },
      { key: "level", label: "숙련도", placeholder: "예: 상/중/하" },
      { key: "category", label: "카테고리", placeholder: "예: 언어, 프레임워크" },
    ],
  },
  {
    key: "certificates",
    title: "자격증",
    endpoint: "/certificates",
    listKey: "certificates",
    titleKey: "name",
    subtitleKeys: ["issuer"],
    fields: [
      { key: "name", label: "자격증명", required: true },
      { key: "issuer", label: "발행기관" },
      { key: "acquiredAt", label: "취득일", type: "date" },
    ],
  },
  {
    key: "introductions",
    title: "자기소개",
    endpoint: "/introductions",
    inline: true,
    listKey: "introductions",
    titleKey: "title",
    subtitleKeys: [],
    fields: [
      { key: "title", label: "항목 제목", required: true, placeholder: "예: 지원 동기, 성장 경험" },
      { key: "content", label: "내용", type: "textarea", required: true },
    ],
  },
  {
    key: "portfolioItems",
    title: "포트폴리오",
    endpoint: "/portfolio-items",
    listKey: "portfolioItems",
    titleKey: "title",
    subtitleKeys: ["itemType", "linkUrl", "filePath"],
    fields: [
      { key: "title", label: "작업물 제목", required: true },
      { key: "itemType", label: "유형", type: "select", options: ["LINK", "FILE"] },
      {
        key: "linkUrl",
        label: "URL (LINK)",
        placeholder: "https://github.com/...",
        showIf: { key: "itemType", equals: "LINK" },
      },
      {
        key: "filePath",
        label: "첨부파일 (FILE) — pdf/pptx/docx/png/jpg, 10MB 이하",
        type: "file",
        accept: ".pdf,.pptx,.ppt,.docx,.png,.jpg,.jpeg",
        showIf: { key: "itemType", equals: "FILE" },
      },
      { key: "description", label: "설명", type: "textarea" },
    ],
  },
];

export default function EditPage({ documentId }: { documentId?: number }) {
  const [view, setView] = useState<ResumeView | null>(null);
  const [documents, setDocuments] = useState<ResumeDocument[]>([]);
  const [templateCode, setTemplateCode] = useState("CLASSIC");
  const [sectionItems, setSectionItems] = useState<SectionItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<ResumeView>("/view")
      .then(setView)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
    apiGet<ResumeDocument[]>("/documents")
      .then((docs) => {
        setDocuments(docs);
        const doc = docs.find((d) => d.id === documentId);
        if (doc) {
          setTemplateCode(doc.templateCode || "CLASSIC");
          try {
            setSectionItems(JSON.parse(doc.sectionConfig) as SectionItem[]);
          } catch {
            setSectionItems(
              DEFAULT_ORDER.map((key, i) => ({ key, included: true, order: i + 1 })),
            );
          }
        }
      })
      .catch(() => undefined);
  }, [documentId]);

  const changeTemplate = async (code: string) => {
    if (!documentId) return;
    setTemplateCode(code);
    try {
      await apiPut(`/documents/${documentId}`, { templateCode: code });
    } catch {
      setError("테마 변경에 실패했습니다.");
    }
  };

  const persistSections = (next: SectionItem[]) => {
    setSectionItems(next);
    if (!documentId) return;
    const body = next
      .map((s) => ({ key: s.key, included: s.included, order: s.order }))
      .sort((a, b) => a.order - b.order);
    apiPut(`/documents/${documentId}`, { sectionConfig: JSON.stringify(body) }).catch(() => {
      setError("섹션 편성을 저장하지 못했습니다.");
    });
  };

  const toggleSection = (key: string) => {
    if (!sectionItems) return;
    persistSections(
      sectionItems.map((s) => (s.key === key ? { ...s, included: !s.included } : s)),
    );
  };

  const moveSection = (key: string, dir: -1 | 1) => {
    if (!sectionItems) return;
    const idx = sectionItems.findIndex((s) => s.key === key);
    const target = idx + dir;
    if (idx < 0 || target < 0 || target >= sectionItems.length) return;
    const next = [...sectionItems];
    const cur = next[idx];
    const other = next[target];
    next[idx] = { ...other, order: cur.order };
    next[target] = { ...cur, order: other.order };
    persistSections(next);
  };

  const orderedSections = useMemo(() => {
    if (sectionItems) {
      return [...sectionItems]
        .sort((a, b) => a.order - b.order)
        .filter((s) => s.included)
        .map((s) => s.key);
    }
    return DEFAULT_ORDER;
  }, [sectionItems]);

  const scrollTo = (key: string) => {
    const el = document.getElementById(`section-${key}`);
    if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  if (error === "UNAUTHORIZED") {
    return (
      <div className="p-10 text-center">
        <p className="mb-4">로그인이 필요합니다.</p>
        <a
          href={`/?redirect=${encodeURIComponent("/resume/")}`}
          className="px-4 py-2 bg-gray-900 text-white rounded hover:bg-gray-700"
        >
          로그인하러 가기
        </a>
      </div>
    );
  }

  if (!view || !sectionItems) {
    return (
      <div className="p-10 text-center text-gray-500">
        {error ? `불러오지 못했습니다. (${error})` : "불러오는 중..."}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 py-6">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex justify-between items-center mb-5">
          <div className="flex items-center gap-3">
            <a
              href="#/resumes"
              className="px-2.5 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
            >
              ← 목록
            </a>
            {documents.length > 0 && (
              <select
                value={documentId ?? documents[0]?.id}
                onChange={(e) => {
                  window.location.hash = `#/r/${e.target.value}/edit`;
                }}
                className="border border-gray-300 rounded px-2 py-1.5 text-sm font-semibold text-slate-800 focus:outline-none focus:border-gray-500"
              >
                {documents.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.title}
                  </option>
                ))}
              </select>
            )}
            {documentId && (
              <select
                value={TEMPLATE_LABELS[templateCode] ? templateCode : "CLASSIC"}
                onChange={(e) => void changeTemplate(e.target.value)}
                className="border border-gray-300 rounded px-2 py-1.5 text-sm text-slate-600 focus:outline-none focus:border-gray-500"
              >
                {TEMPLATE_OPTIONS.map((t) => (
                  <option key={t} value={t}>
                    테마: {TEMPLATE_LABELS[t]}
                  </option>
                ))}
              </select>
            )}
          </div>
          <div className="flex gap-2">
            <a
              href={documentId ? `#/r/${documentId}` : "#/resumes"}
              className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
            >
              미리보기
            </a>
            <button
              onClick={logout}
              className="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50"
            >
              로그아웃
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_240px] gap-6 items-start">
          <div className="min-w-0 space-y-6">
            <ProfileEditor
              profile={view.profile}
              onChanged={() =>
                apiGet<ResumeView>("/view").then(setView).catch(() => undefined)
              }
            />

            {orderedSections.map((key) => {
              const cfg = SECTIONS.find((s) => s.key === key)!;
              return (
                <div key={cfg.key} id={`section-${cfg.key}`} className="scroll-mt-4">
                  <CrudSection
                    title={cfg.title}
                    endpoint={cfg.endpoint}
                    items={view[cfg.listKey] as unknown as Record<string, unknown>[]}
                    fields={cfg.fields}
                    titleKey={cfg.titleKey}
                    subtitleKeys={cfg.subtitleKeys}
                    fixedPayload={cfg.fixedPayload}
                    inline={cfg.inline}
                    onChanged={() => {
                      apiGet<ResumeView>("/view").then(setView).catch(() => undefined);
                    }}
                  />
                </div>
              );
            })}
          </div>

          <aside className="lg:sticky lg:top-4 bg-white rounded-xl border border-slate-200 p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-3 flex items-center justify-between">
              구성 편성
              {sectionItems.length > 0 && (
                <span className="text-[11px] font-normal text-slate-400">
                  {sectionItems.filter((s) => s.included).length}/{sectionItems.length} 섹션
                </span>
              )}
            </h3>
            <ul className="space-y-1">
              {[...sectionItems]
                .sort((a, b) => a.order - b.order)
                .map((s, i, arr) => {
                  const visible = orderedSections.includes(s.key);
                  return (
                    <li
                      key={s.key}
                      className={`flex items-center gap-1 rounded px-1 py-1 ${
                        visible ? "hover:bg-gray-50" : "opacity-50"
                      }`}
                    >
                      <div className="flex flex-col">
                        <button
                          onClick={() => moveSection(s.key, -1)}
                          disabled={i === 0}
                          title="위로"
                          className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:hover:text-slate-400 leading-none"
                        >
                          ▲
                        </button>
                        <button
                          onClick={() => moveSection(s.key, 1)}
                          disabled={i === arr.length - 1}
                          title="아래로"
                          className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:hover:text-slate-400 leading-none"
                        >
                          ▼
                        </button>
                      </div>
                      <button
                        onClick={() => {
                          if (visible) scrollTo(s.key);
                        }}
                        className={`flex-1 text-left text-sm truncate ${
                          visible ? "text-slate-700" : "text-slate-400 line-through"
                        }`}
                        title={visible ? SECTION_LABELS[s.key] : `${SECTION_LABELS[s.key]} (숨김)`}
                      >
                        {SECTION_LABELS[s.key] ?? s.key}
                      </button>
                      <button
                        onClick={() => toggleSection(s.key)}
                        title={s.included ? "미리보기에서 숨기기" : "표시하기"}
                        className={`text-xs px-1.5 py-0.5 rounded ${
                          s.included
                            ? "bg-green-50 text-green-600 hover:bg-green-100"
                            : "bg-gray-100 text-gray-400 hover:bg-gray-200"
                        }`}
                      >
                        {s.included ? "보임" : "숨김"}
                      </button>
                    </li>
                  );
                })}
            </ul>
            <p className="mt-3 text-[11px] text-slate-400 leading-snug">
              섹션 표시/순서를 바꾸면 이 이력서의 미리보기·PDF에만 적용됩니다. 항목 데이터는
              모든 이력서가 공유합니다.
            </p>
          </aside>
        </div>
      </div>
    </div>
  );
}
