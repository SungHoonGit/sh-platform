import { useEffect, useState } from "react";
import { apiDownloadShare, apiGetShare } from "../api/client";
import type { ResumeView } from "../types/resume";
import type { SectionItem } from "../types/document";
import ClassicTemplate from "../components/templates/ClassicTemplate";
import ModernTemplate from "../components/templates/ModernTemplate";
import SaraminTemplate from "../components/templates/SaraminTemplate";
import { DEFAULT_ORDER } from "../components/templates/shared";

interface ShareViewData {
  documentId: number;
  title: string;
  templateCode: string;
  sectionConfig: string;
  view: ResumeView;
}

export default function ShareViewPage({ token }: { token: string }) {
  const [data, setData] = useState<ShareViewData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGetShare<ShareViewData>(`/${token}`)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, [token]);

  if (error) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="bg-white border border-gray-200 rounded-xl p-8 text-center">
          <p className="text-lg font-semibold text-slate-800 mb-1">잘못된 공유 링크입니다</p>
          <p className="text-sm text-slate-500">
            링크가 만료되었거나 공유가 해제되었을 수 있습니다.
          </p>
        </div>
      </div>
    );
  }

  if (!data) {
    return <div className="min-h-screen bg-gray-100 flex items-center justify-center text-gray-500">불러오는 중...</div>;
  }

  const { view, templateCode, sectionConfig } = data;
  const config: SectionItem[] = (() => {
    try {
      return JSON.parse(sectionConfig);
    } catch {
      return [];
    }
  })();
  const order: string[] =
    config.length > 0
      ? config
          .filter((s) => s.included)
          .sort((a, b) => a.order - b.order)
          .map((s) => s.key)
      : DEFAULT_ORDER;

  return (
    <div className="min-h-screen bg-gray-100 py-6 print:bg-white print:py-0">
      <style>{`@media print { @page { margin: 15mm; } }`}</style>

      <div className="max-w-3xl mx-auto mb-4 flex justify-end items-center gap-2 print:hidden">
        <span className="mr-auto text-sm text-slate-500">{data.title}</span>
        <a
          href={`#/s/${token}`}
          className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
        >
          공유된 이력서
        </a>
        <button
          onClick={() =>
            apiDownloadShare(`/${token}/pdf`, `${data.title}.pdf`).catch((e) =>
              alert(`PDF 다운로드 실패: ${e.message}`),
            )
          }
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