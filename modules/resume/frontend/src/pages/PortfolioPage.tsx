import { useCallback, useEffect, useState } from "react";
import { apiGet } from "../api/client";
import type { PortfolioItem } from "../types/resume";
import CrudSection from "../components/CrudSection";
import {
  PORTFOLIO_ENDPOINT,
  PORTFOLIO_FIELDS,
} from "../config/portfolioConfig";

export default function PortfolioPage() {
  const [documents, setDocuments] = useState<{ id: number; title: string }[]>([]);
  const [documentId, setDocumentId] = useState<number | null>(null);
  const [items, setItems] = useState<PortfolioItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<{ id: number; title: string }[]>("/documents")
      .then((docs) => {
        setDocuments(docs);
        setDocumentId((prev) => prev ?? (docs[0]?.id ?? null));
      })
      .catch(() => undefined);
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    const params = documentId ? { documentId: String(documentId) } : undefined;
    apiGet<PortfolioItem[]>(PORTFOLIO_ENDPOINT, params)
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [documentId]);

  useEffect(load, [load]);

  return (
    <div className="min-h-screen bg-gray-100 py-6">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex justify-between items-center mb-4 flex-wrap gap-2">
          <div>
            <h1 className="text-xl font-bold text-slate-900">프로젝트 관리</h1>
            <p className="mt-1 text-sm text-slate-500">
              작업물을 이력서와 독립적으로 관리합니다. 이력서의 프로젝트 &quot;작업물에서
              가져오기&quot;와 포트폴리오 섹션에서 사용됩니다.
            </p>
          </div>
          <label className="text-sm text-slate-600 flex items-center gap-2">
            적용할 이력서
            <select
              value={documentId ?? ""}
              onChange={(e) => setDocumentId(e.target.value ? Number(e.target.value) : null)}
              className="border border-gray-300 rounded px-2 py-1.5 bg-white"
            >
              {documents.length === 0 && <option value="">문서 없음</option>}
              {documents.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.title || `이력서 #${d.id}`}
                </option>
              ))}
            </select>
          </label>
        </div>

        {loading ? (
          <div className="p-10 text-center text-gray-500">불러오는 중...</div>
        ) : (
          <CrudSection
            title="내 프로젝트"
            endpoint={PORTFOLIO_ENDPOINT}
            items={items as unknown as Record<string, unknown>[]}
            fields={PORTFOLIO_FIELDS}
            titleKey="title"
            subtitleKeys={["description"]}
            documentId={documentId ?? undefined}
            onChanged={load}
          />
        )}
      </div>
    </div>
  );
}