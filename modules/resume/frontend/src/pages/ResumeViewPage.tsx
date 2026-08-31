import { useEffect, useState } from "react";
import { apiDownload, apiGet } from "../api/client";
import type { ResumeView } from "../types/resume";
import type { SectionItem } from "../types/document";
import ClassicTemplate from "../components/templates/ClassicTemplate";
import ModernTemplate from "../components/templates/ModernTemplate";
import SaraminTemplate from "../components/templates/SaraminTemplate";
import { DEFAULT_ORDER, TEMPLATE_LABELS } from "../components/templates/shared";

export default function ResumeViewPage({ documentId }: { documentId?: number }) {
  const [view, setView] = useState<ResumeView | null>(null);
  const [templateCode, setTemplateCode] = useState("CLASSIC");
  const [sectionOrder, setSectionOrder] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<ResumeView>("/view")
      .then(setView)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
    if (documentId) {
      apiGet<{ id: number; templateCode: string; sectionConfig: string }[]>("/documents")
        .then((docs) => {
          const doc = docs.find((d) => d.id === documentId);
          if (!doc) return;
          setTemplateCode(doc.templateCode || "CLASSIC");
          const config: SectionItem[] = JSON.parse(doc.sectionConfig);
          setSectionOrder(
            config
              .filter((s) => s.included)
              .sort((a, b) => a.order - b.order)
              .map((s) => s.key),
          );
        })
        .catch(() => undefined);
    }
  }, [documentId]);

  if (loading) {
    return <div className="p-10 text-center text-gray-500">불러오는 중...</div>;
  }

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

  if (error || !view) {
    return <div className="p-10 text-center text-red-500">이력서를 불러오지 못했습니다. ({error})</div>;
  }

  const order = sectionOrder ?? DEFAULT_ORDER;

  return (
    <div className="min-h-screen bg-gray-100 py-6 print:bg-white print:py-0">
      <style>{`@media print { @page { margin: 15mm; } }`}</style>

      <div className="max-w-3xl mx-auto mb-4 flex justify-end items-center gap-2 print:hidden">
        {documentId && (
          <span className="mr-auto text-sm text-slate-500">
            테마: <b className="text-slate-700">{TEMPLATE_LABELS[templateCode] ?? templateCode}</b>
          </span>
        )}
        <a
          href="#/resumes"
          className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
        >
          목록
        </a>
        <a
          href={documentId ? `#/r/${documentId}/edit` : "#/resumes"}
          className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
        >
          편집
        </a>
        <button
          onClick={() => {
            const url = documentId ? `/view/pdf?documentId=${documentId}` : "/view/pdf";
            apiDownload(url, "resume.pdf").catch((e) => {
              if (e.message !== "UNAUTHORIZED") alert(`PDF 다운로드 실패: ${e.message}`);
            });
          }}
          className="px-3 py-1.5 text-sm bg-gray-900 text-white rounded hover:bg-gray-700"
        >
          PDF 다운로드
        </button>
      </div>

      <div className="max-w-3xl mx-auto bg-white shadow-sm px-10 py-8 print:shadow-none print:px-0 print:max-w-none">
        {templateCode === "MODERN" ? (
          <ModernTemplate view={view} order={order} />
        ) : templateCode === "SARAMIN" ? (
          <SaraminTemplate view={view} order={order} />
        ) : (
          <ClassicTemplate view={view} order={order} />
        )}
      </div>
    </div>
  );
}
