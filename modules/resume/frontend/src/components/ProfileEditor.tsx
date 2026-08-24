import { useEffect, useState } from "react";
import { apiPut, apiUpload } from "../api/client";
import type { Profile } from "../types/resume";

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

declare global {
  interface Window {
    daum?: {
      Postcode: new (options: {
        oncomplete: (data: { zonecode: string; roadAddress: string; jibunAddress: string }) => void;
      }) => { open: () => void };
    };
  }
}

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
  const [photoBusy, setPhotoBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setForm((prev) => ({ ...prev, photoUrl: profile?.photoUrl ?? "" }));
  }, [profile?.photoUrl]);

  const uploadPhoto = async (file: File) => {
    setPhotoBusy(true);
    setError(null);
    try {
      await apiUpload("/profile/photo", file);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "사진 업로드에 실패했습니다.");
    } finally {
      setPhotoBusy(false);
    }
  };

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
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-slate-600 mb-1">주소</label>
          <div className="flex gap-2">
            <input value={form.address} onChange={(e) => setVal("address", e.target.value)} className={inputCls} />
            <button
              type="button"
              onClick={() => {
                if (!window.daum) {
                  alert("주소 검색 모듈을 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.");
                  return;
                }
                new window.daum.Postcode({
                  oncomplete: (data) => {
                    const base = data.roadAddress || data.jibunAddress;
                    setVal("address", `${data.zonecode} ${base}`);
                  },
                }).open();
              }}
              className="shrink-0 px-3 py-1.5 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50"
            >
              주소 검색
            </button>
          </div>
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-slate-600 mb-1">프로필 사진 (jpg/png)</label>
          <div className="flex items-center gap-3">
            <input
              type="file"
              accept=".jpg,.jpeg,.png"
              disabled={photoBusy}
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) void uploadPhoto(file);
                e.target.value = "";
              }}
              className="text-sm text-slate-600 file:mr-2 file:px-2.5 file:py-1.5 file:text-xs file:border-0 file:bg-slate-900 file:text-white file:rounded hover:file:bg-slate-700"
            />
            {photoBusy && <span className="text-xs text-slate-500">업로드 중...</span>}
            {form.photoUrl && !photoBusy && (
              <span className="text-xs text-green-700">✓ 사진 등록됨</span>
            )}
          </div>
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
