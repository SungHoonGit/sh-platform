import { useCallback, useEffect, useState } from "react";
import { apiGet } from "../api/client";
import type { PortfolioItem } from "../types/resume";
import CrudSection from "../components/CrudSection";
import {
  PORTFOLIO_ENDPOINT,
  PORTFOLIO_FIELDS,
} from "../config/portfolioConfig";

export default function PortfolioPage() {
  const [items, setItems] = useState<PortfolioItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    apiGet<PortfolioItem[]>(PORTFOLIO_ENDPOINT)
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  return (
    <div className="min-h-screen bg-gray-100 py-6">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex justify-between items-center mb-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">포트폴리오 관리</h1>
            <p className="mt-1 text-sm text-slate-500">
              작업물을 이력서와 독립적으로 관리합니다. 모든 이력서의 포트폴리오 섹션과
              프로젝트의 &quot;작업물에서 가져오기&quot;에서 사용됩니다.
            </p>
          </div>
        </div>

        {loading ? (
          <div className="p-10 text-center text-gray-500">불러오는 중...</div>
        ) : (
          <CrudSection
            title="포트폴리오 작업물"
            endpoint={PORTFOLIO_ENDPOINT}
            items={items as unknown as Record<string, unknown>[]}
            fields={PORTFOLIO_FIELDS}
            titleKey="title"
            subtitleKeys={["description"]}
            onChanged={load}
          />
        )}
      </div>
    </div>
  );
}