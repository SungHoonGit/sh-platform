import { useCallback, useEffect, useState } from "react";
import { apiGet, logout } from "../api/client";
import type { ResumeView } from "../types/resume";
import CrudSection, { type FieldDef } from "../components/CrudSection";
import ProfileEditor from "../components/ProfileEditor";

interface SectionConfig {
  title: string;
  endpoint: string;
  listKey: keyof ResumeView;
  titleKey: string;
  subtitleKeys: string[];
  fields: FieldDef[];
  fixedPayload?: Record<string, unknown>;
}

const SECTIONS: SectionConfig[] = [
  {
    title: "경력",
    endpoint: "/careers",
    listKey: "careers",
    titleKey: "company",
    subtitleKeys: ["title", "description"],
    fields: [
      { key: "company", label: "회사명", required: true },
      { key: "title", label: "직무/직책" },
      { key: "startDate", label: "입사일", type: "date" },
      { key: "endDate", label: "퇴사일", type: "date", placeholder: "재직 중이면 비움" },
      { key: "description", label: "주요 업무", type: "textarea" },
    ],
  },
  {
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
    title: "학력",
    endpoint: "/educations",
    listKey: "educations",
    titleKey: "school",
    subtitleKeys: ["major"],
    fields: [
      { key: "school", label: "학교명", required: true },
      { key: "major", label: "전공" },
      { key: "degree", label: "학위", placeholder: "예: 학사" },
      { key: "startDate", label: "입학일", type: "date" },
      { key: "endDate", label: "졸업일", type: "date" },
      { key: "status", label: "상태", placeholder: "예: 졸업, 재학" },
    ],
  },
  {
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
    title: "자기소개",
    endpoint: "/introductions",
    listKey: "introductions",
    titleKey: "title",
    subtitleKeys: [],
    fields: [
      { key: "title", label: "항목 제목", required: true, placeholder: "예: 지원 동기, 성장 경험" },
      { key: "content", label: "내용", type: "textarea", required: true },
    ],
  },
  {
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

export default function EditPage() {
  const [view, setView] = useState<ResumeView | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    apiGet<ResumeView>("/view")
      .then(setView)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

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

  if (!view) {
    return (
      <div className="p-10 text-center text-gray-500">
        {error ? `불러오지 못했습니다. (${error})` : "불러오는 중..."}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 py-6">
      <div className="max-w-3xl mx-auto px-4">
        <div className="flex justify-between items-center mb-5">
          <h1 className="text-xl font-bold text-slate-800">이력서 편집</h1>
          <div className="flex gap-2">
            <a
              href="#"
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

        <ProfileEditor profile={view.profile} onChanged={load} />

        {SECTIONS.map((cfg) => (
          <CrudSection
            key={cfg.endpoint}
            title={cfg.title}
            endpoint={cfg.endpoint}
            items={view[cfg.listKey] as unknown as Record<string, unknown>[]}
            fields={cfg.fields}
            titleKey={cfg.titleKey}
            subtitleKeys={cfg.subtitleKeys}
            fixedPayload={cfg.fixedPayload}
            onChanged={load}
          />
        ))}
      </div>
    </div>
  );
}
