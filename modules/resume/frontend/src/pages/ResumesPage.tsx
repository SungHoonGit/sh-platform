import { useCallback, useEffect, useState } from "react";
import { apiDelete, apiGet, apiPost, apiPut } from "../api/client";
import type { ResumeDocument } from "../types/document";
import { TEMPLATE_LABELS, TEMPLATE_OPTIONS } from "../components/templates/shared";

export default function ResumesPage() {
  const [documents, setDocuments] = useState<ResumeDocument[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState("");
  const [fromId, setFromId] = useState<string>("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    apiGet<ResumeDocument[]>("/documents")
      .then(setDocuments)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const create = async () => {
    if (!title.trim()) return;
    setBusy(true);
    try {
      await apiPost("/documents", {
        title: title.trim(),
        fromDocumentId: fromId ? Number(fromId) : null,
      });
      setTitle("");
      setFromId("");
      setCreating(false);
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "생성에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number, name: string) => {
    if (!(await window.confirm(`'${name}' 을(를) 삭제할까요?`))) return;
    try {
      await apiDelete(`/documents/${id}`);
      load();
    } catch (e) {
      setError(
        e instanceof Error && e.message === "API_ERROR_400"
          ? "마지막 이력서는 삭제할 수 없습니다."
          : "삭제에 실패했습니다.",
      );
    }
  };

  const markPrimary = async (id: number) => {
    try {
      await apiPut(`/documents/${id}/primary`, {});
      load();
    } catch {
      setError("대표 지정에 실패했습니다.");
    }
  };

  const changeTemplate = async (id: number, templateCode: string) => {
    try {
      await apiPut(`/documents/${id}`, { templateCode });
      load();
    } catch {
      setError("테마 변경에 실패했습니다.");
    }
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

  if (!documents) {
    return (
      <div className="p-10 text-center text-slate-500">
        {error ? `불러오지 못했습니다. (${error})` : "불러오는 중..."}
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-2">
        <h1 className="text-xl font-bold text-slate-800">이력서 관리</h1>
        <button
          onClick={() => setCreating((v) => !v)}
          className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700"
        >
          {creating ? "취소" : "+ 새 이력서"}
        </button>
      </div>
      <p className="text-xs text-slate-400 mb-4">
        이력서는 항목 데이터(경력·학력·자기소개 등)를 <b className="text-slate-500">모든 이력서가 공유</b>하고,
        각 이력서마다 <b className="text-slate-500">구성 섹션·순서·테마</b>를 다르게 편성합니다. 중복처럼 보여도 별도 서식입니다.
      </p>

      {error && (
        <p className="mb-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
          {error}
        </p>
      )}

      {creating && (
        <div className="mb-6 bg-white border border-slate-200 rounded-xl p-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                제목<span className="text-red-500 ml-0.5">*</span>
              </label>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 사람인용 요약본"
                className="w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                구성 불러오기 (선택)
              </label>
              <select
                value={fromId}
                onChange={(e) => setFromId(e.target.value)}
                className="w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500"
              >
                <option value="">빈 상태로 시작</option>
                {documents.map((d) => (
                  <option key={d.id} value={String(d.id)}>
                    '{d.title}'의 구성(섹션·테마) 불러오기
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button
              onClick={create}
              disabled={busy || !title.trim()}
              className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700 disabled:opacity-50"
            >
              {busy ? "생성 중..." : "만들기"}
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {documents.map((d) => (
          <div
            key={d.id}
            className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col hover:shadow-md transition-shadow"
          >
            <div className="flex items-start justify-between gap-2 mb-2">
              <h2 className="font-semibold text-slate-800 truncate">{d.title}</h2>
              {d.primary && (
                <span className="shrink-0 px-1.5 py-0.5 bg-blue-50 text-blue-700 text-[10px] font-medium rounded">
                  대표
                </span>
              )}
            </div>
            <select
              value={TEMPLATE_LABELS[d.templateCode] ? d.templateCode : "CLASSIC"}
              onChange={(e) => changeTemplate(d.id, e.target.value)}
              className="mb-3 w-full border border-gray-200 rounded px-1.5 py-1 text-xs text-slate-600 focus:outline-none focus:border-gray-400"
            >
              {TEMPLATE_OPTIONS.map((t) => (
                <option key={t} value={t}>
                  테마: {TEMPLATE_LABELS[t]}
                </option>
              ))}
            </select>
            <div className="mt-auto flex flex-wrap gap-1.5">
              <a
                href={`#/r/${d.id}`}
                className="px-2.5 py-1 text-xs bg-slate-900 text-white rounded hover:bg-slate-700"
              >
                보기
              </a>
              <a
                href={`#/r/${d.id}/edit`}
                className="px-2.5 py-1 text-xs border border-gray-300 rounded hover:bg-gray-50"
              >
                편집
              </a>
              {!d.primary && (
                <button
                  onClick={() => markPrimary(d.id)}
                  className="px-2.5 py-1 text-xs border border-blue-200 text-blue-700 rounded hover:bg-blue-50"
                >
                  대표 지정
                </button>
              )}
              <button
                onClick={() => remove(d.id, d.title)}
                className="px-2.5 py-1 text-xs border border-red-200 text-red-600 rounded hover:bg-red-50"
              >
                삭제
              </button>
            </div>
          </div>
        ))}
      </div>

      {documents.length === 0 && (
        <p className="text-center text-slate-400 mt-10">
          아직 이력서가 없습니다. [새 이력서]로 만들어보세요.
        </p>
      )}
    </div>
  );
}
