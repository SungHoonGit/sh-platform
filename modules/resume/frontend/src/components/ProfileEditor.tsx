import { useState } from "react";
import { apiPut } from "../api/client";
import type { Profile } from "../types/resume";

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

export default function ProfileEditor({
  profile,
  onChanged,
}: {
  profile: Profile | null;
  onChanged: () => void;
}) {
  const [form, setForm] = useState<Record<string, string>>({
    name: profile?.name ?? "",
    headline: profile?.headline ?? "",
    email: profile?.email ?? "",
    phone: profile?.phone ?? "",
    birthDate: profile?.birthDate ?? "",
    address: profile?.address ?? "",
    photoUrl: profile?.photoUrl ?? "",
  });
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setVal = (k: string, v: string) => {
    setSaved(false);
    setForm((prev) => ({ ...prev, [k]: v }));
  };

  const save = async () => {
    setBusy(true);
    setError(null);
    try {
      await apiPut("/profile", {
        name: form.name || null,
        headline: form.headline || null,
        email: form.email || null,
        phone: form.phone || null,
        birthDate: form.birthDate || null,
        address: form.address || null,
        photoUrl: form.photoUrl || null,
      });
      setSaved(true);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mb-6 bg-white rounded-xl border border-slate-200 p-5">
      <h2 className="font-bold text-slate-800 mb-3">인적사항</h2>
      {error && (
        <p className="mb-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">{error}</p>
      )}
      {saved && (
        <p className="mb-3 text-sm text-green-700 bg-green-50 border border-green-200 rounded px-3 py-2">
          저장되었습니다.
        </p>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">이름</label>
          <input value={form.name} onChange={(e) => setVal("name", e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">한 줄 소개</label>
          <input
            value={form.headline}
            onChange={(e) => setVal("headline", e.target.value)}
            placeholder="예: 백엔드 개발자 (Java/Spring)"
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">이메일</label>
          <input type="email" value={form.email} onChange={(e) => setVal("email", e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">전화번호</label>
          <input value={form.phone} onChange={(e) => setVal("phone", e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">생년월일</label>
          <input type="date" value={form.birthDate} onChange={(e) => setVal("birthDate", e.target.value)} className={inputCls} />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">주소</label>
          <input value={form.address} onChange={(e) => setVal("address", e.target.value)} className={inputCls} />
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-slate-600 mb-1">프로필 사진 URL</label>
          <input
            value={form.photoUrl}
            onChange={(e) => setVal("photoUrl", e.target.value)}
            placeholder="(선택)"
            className={inputCls}
          />
        </div>
      </div>
      <div className="mt-4 flex justify-end">
        <button
          onClick={save}
          disabled={busy}
          className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700 disabled:opacity-50"
        >
          {busy ? "저장 중..." : "저장"}
        </button>
      </div>
    </section>
  );
}
