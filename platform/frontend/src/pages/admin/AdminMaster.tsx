import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, X, Check } from "lucide-react";
import { masterApi, regionApi, searchMappingApi } from "../../api/master";
import type { SiteDefinition, Region, SearchMapping, ValueType, RegionRequest } from "../../api/master";

type Tab = "sites" | "regions" | "mappings";

export default function AdminMaster() {
  const [tab, setTab] = useState<Tab>("sites");
  const tabs: { key: Tab; label: string }[] = [
    { key: "sites", label: "사이트" },
    { key: "regions", label: "지역" },
    { key: "mappings", label: "검색 매핑" },
  ];

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">마스터 관리</h1>
      <p className="text-slate-500 mb-6">
        서비스에 노출되는 기준 데이터입니다. 스크래퍼 검색 매핑은 배포 없이 즉시 반영됩니다.
      </p>

      <div className="flex gap-2 mb-6">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              tab === t.key ? "bg-blue-600 text-white" : "bg-white border border-slate-300 text-slate-600 hover:bg-slate-100"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "sites" && <SitesTab />}
      {tab === "regions" && <RegionsTab />}
      {tab === "mappings" && <MappingsTab />}
    </div>
  );
}

// ── 사이트 ────────────────────────────────────────────────────────

function SitesTab() {
  const qc = useQueryClient();
  const { data: sites, isLoading } = useQuery({
    queryKey: ["master-sites"],
    queryFn: masterApi.getSites,
  });

  const saveMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<SiteDefinition> }) => masterApi.updateSite(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["master-sites"] }),
    onError: () => alert("사이트 저장 실패: 표시명과 사이트 코드는 필수입니다."),
  });

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<Partial<SiteDefinition>>({});

  const startEdit = (s: SiteDefinition) => {
    setEditingId(s.id);
    setDraft({ displayName: s.displayName, displayOrder: s.displayOrder, isEnabled: s.isEnabled, icon: s.icon ?? "", color: s.color ?? "" });
  };

  const save = () => {
    if (editingId == null) return;
    saveMutation.mutate({ id: editingId, data: draft });
    setEditingId(null);
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <table className="w-full">
        <thead>
          <tr className="bg-slate-50 border-b border-slate-200">
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">사이트</th>
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">표시명</th>
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">순서</th>
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">아이콘/색</th>
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">활성</th>
            <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">관리</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {isLoading ? (
            <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">로딩 중...</td></tr>
          ) : !sites?.length ? (
            <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">사이트가 없습니다</td></tr>
          ) : (
            sites.map((s) =>
              editingId === s.id ? (
                <tr key={s.id} className="bg-blue-50/50">
                  <td className="px-4 py-3 text-sm font-medium text-slate-800">{s.siteName}</td>
                  <td className="px-4 py-3">
                    <input
                      value={draft.displayName ?? ""}
                      onChange={(e) => setDraft((d) => ({ ...d, displayName: e.target.value }))}
                      className="w-full px-2 py-1 border border-slate-300 rounded-md text-sm"
                    />
                  </td>
                  <td className="px-4 py-3">
                    <input
                      type="number"
                      value={draft.displayOrder ?? 0}
                      onChange={(e) => setDraft((d) => ({ ...d, displayOrder: Number(e.target.value) }))}
                      className="w-16 px-2 py-1 border border-slate-300 rounded-md text-sm"
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1">
                      <input
                        value={draft.icon ?? ""}
                        onChange={(e) => setDraft((d) => ({ ...d, icon: e.target.value }))}
                        placeholder="icon"
                        className="w-20 px-2 py-1 border border-slate-300 rounded-md text-sm"
                      />
                      <input
                        value={draft.color ?? ""}
                        onChange={(e) => setDraft((d) => ({ ...d, color: e.target.value }))}
                        placeholder="color"
                        className="w-20 px-2 py-1 border border-slate-300 rounded-md text-sm"
                      />
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={!!draft.isEnabled}
                      onChange={(e) => setDraft((d) => ({ ...d, isEnabled: e.target.checked }))}
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button onClick={save} className="text-green-600 hover:text-green-800 p-1"><Check size={16} /></button>
                      <button onClick={() => setEditingId(null)} className="text-slate-400 hover:text-slate-600 p-1"><X size={16} /></button>
                    </div>
                  </td>
                </tr>
              ) : (
                <tr key={s.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm font-medium text-slate-800">{s.siteName}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{s.displayName}</td>
                  <td className="px-4 py-3 text-sm text-slate-500">{s.displayOrder}</td>
                  <td className="px-4 py-3 text-sm text-slate-500">
                    {[s.icon, s.color].filter(Boolean).join(" / ") || "-"}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {s.isEnabled ? <span className="text-green-600">활성</span> : <span className="text-slate-400">비활성</span>}
                  </td>
                  <td className="px-4 py-3">
                    <button onClick={() => startEdit(s)} className="text-blue-500 hover:text-blue-700 p-1">
                      <Pencil size={16} />
                    </button>
                  </td>
                </tr>
              )
            )
          )}
        </tbody>
      </table>
    </div>
  );
}

// ── 지역 ─────────────────────────────────────────────────────────

function RegionsTab() {
  const qc = useQueryClient();
  const { data: regions, isLoading } = useQuery({
    queryKey: ["master-regions"],
    queryFn: regionApi.getRegions,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["master-regions"] });
  const createMutation = useMutation({ mutationFn: regionApi.createRegion, onSuccess: invalidate, onError: () => alert("지역 추가 실패: 중복 이름이거나 입력이 올바르지 않습니다.") });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: RegionRequest }) =>
      regionApi.updateRegion(id, { name: data.name, displayOrder: data.displayOrder, isActive: data.isActive }),
    onSuccess: invalidate,
    onError: () => alert("지역 수정 실패."),
  });
  const deleteMutation = useMutation({ mutationFn: regionApi.deleteRegion, onSuccess: invalidate, onError: () => alert("지역 삭제 실패.") });

  const [newName, setNewName] = useState("");
  const [newOrder, setNewOrder] = useState(18);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<Partial<Region>>({});

  const add = () => {
    if (!newName.trim()) return;
    createMutation.mutate({ name: newName.trim(), displayOrder: newOrder });
    setNewName("");
  };

  const startEdit = (r: Region) => {
    setEditingId(r.id);
    setDraft({ name: r.name, displayOrder: r.displayOrder, isActive: r.isActive });
  };

  const save = () => {
    if (editingId == null) return;
    if (!draft.name?.trim()) return;
    updateMutation.mutate({ id: editingId, data: { name: draft.name.trim(), displayOrder: draft.displayOrder, isActive: draft.isActive } });
    setEditingId(null);
  };

  return (
    <div>
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 mb-4 flex items-end gap-3">
        <div>
          <label className="block text-xs text-slate-500 mb-1">지역명</label>
          <input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="예: 울릉도"
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
        </div>
        <div>
          <label className="block text-xs text-slate-500 mb-1">표시 순서</label>
          <input type="number" value={newOrder} onChange={(e) => setNewOrder(Number(e.target.value))}
            className="w-20 px-3 py-2 border border-slate-300 rounded-lg text-sm" />
        </div>
        <button onClick={add} className="px-3 py-2 bg-blue-600 text-white rounded-lg text-sm flex items-center gap-1">
          <Plus size={16} /> 추가
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">이름</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">순서</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">활성</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr><td colSpan={4} className="px-4 py-8 text-center text-slate-400">로딩 중...</td></tr>
            ) : !regions?.length ? (
              <tr><td colSpan={4} className="px-4 py-8 text-center text-slate-400">지역이 없습니다</td></tr>
            ) : (
              regions.map((r) =>
                editingId === r.id ? (
                  <tr key={r.id} className="bg-blue-50/50">
                    <td className="px-4 py-3">
                      <input value={draft.name ?? ""} onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
                        className="px-2 py-1 border border-slate-300 rounded-md text-sm" />
                    </td>
                    <td className="px-4 py-3">
                      <input type="number" value={draft.displayOrder ?? 0} onChange={(e) => setDraft((d) => ({ ...d, displayOrder: Number(e.target.value) }))}
                        className="w-16 px-2 py-1 border border-slate-300 rounded-md text-sm" />
                    </td>
                    <td className="px-4 py-3">
                      <input type="checkbox" checked={!!draft.isActive} onChange={(e) => setDraft((d) => ({ ...d, isActive: e.target.checked }))} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button onClick={save} className="text-green-600 hover:text-green-800 p-1"><Check size={16} /></button>
                        <button onClick={() => setEditingId(null)} className="text-slate-400 hover:text-slate-600 p-1"><X size={16} /></button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  <tr key={r.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-sm font-medium text-slate-800">{r.name}</td>
                    <td className="px-4 py-3 text-sm text-slate-500">{r.displayOrder}</td>
                    <td className="px-4 py-3 text-sm">{r.isActive ? <span className="text-green-600">활성</span> : <span className="text-slate-400">비활성</span>}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        <button onClick={() => startEdit(r)} className="text-blue-500 hover:text-blue-700 p-1"><Pencil size={16} /></button>
                        <button onClick={async () => { if (await confirm(`지역 '${r.name}'을 삭제하시겠습니까?`)) deleteMutation.mutate(r.id); }}
                          className="text-red-500 hover:text-red-700 p-1"><Trash2 size={16} /></button>
                      </div>
                    </td>
                  </tr>
                )
              )
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 검색 매핑 ────────────────────────────────────────────────────

function MappingsTab() {
  const qc = useQueryClient();
  const { data: sites } = useQuery({ queryKey: ["master-sites"], queryFn: masterApi.getSites });
  const [site, setSite] = useState("saramin");

  const { data: mappings, isLoading } = useQuery({
    queryKey: ["master-mappings", site],
    queryFn: () => searchMappingApi.listBySite(site),
    enabled: !!site,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["master-mappings", site] });
  const createMutation = useMutation({
    mutationFn: (data: { standardKey: string; urlParamName: string; valueType: ValueType; valueMapping: string; displayOrder: number }) =>
      searchMappingApi.create({
        siteDefinitionId: sites?.find((s) => s.siteName === site)?.id ?? null,
        standardKey: data.standardKey,
        urlParamName: data.urlParamName,
        valueType: data.valueType,
        valueMapping: data.valueType === "direct" ? null : data.valueMapping,
        displayOrder: data.displayOrder,
      }),
    onSuccess: invalidate,
    onError: () => alert("매핑 추가 실패: (사이트, 키) 중복이거나 value_mapping이 올바른 JSON이 아닙니다."),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<SearchMapping> }) =>
      searchMappingApi.update(id, {
        urlParamName: data.urlParamName!,
        valueType: data.valueType!,
        valueMapping: data.valueType === "direct" ? null : (data.valueMapping ?? null),
        isEnabled: data.isEnabled,
        displayOrder: data.displayOrder,
      }),
    onSuccess: invalidate,
    onError: () => alert("매핑 수정 실패."),
  });
  const deleteMutation = useMutation({ mutationFn: searchMappingApi.delete, onSuccess: invalidate, onError: () => alert("매핑 삭제 실패.") });

  // 추가 폼
  const [key, setKey] = useState("");
  const [param, setParam] = useState("");
  const [type, setType] = useState<ValueType>("mapped");
  const [mapping, setMapping] = useState("");
  const [order, setOrder] = useState(10);

  const add = () => {
    if (!key.trim() || !param.trim()) return;
    createMutation.mutate({ standardKey: key.trim(), urlParamName: param.trim(), valueType: type, valueMapping: mapping, displayOrder: order });
    setKey(""); setParam(""); setMapping(""); setOrder(10); setType("mapped");
  };

  const addForm = (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 mb-4 flex flex-wrap items-end gap-3">
      <div>
        <label className="block text-xs text-slate-500 mb-1">표준 키</label>
        <input value={key} onChange={(e) => setKey(e.target.value)} placeholder="예: location" className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500 mb-1">URL 파라미터</label>
        <input value={param} onChange={(e) => setParam(e.target.value)} placeholder="예: loc_cd" className="px-3 py-2 border border-slate-300 rounded-lg text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500 mb-1">변환 방식</label>
        <select value={type} onChange={(e) => setType(e.target.value as ValueType)} className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white">
          <option value="direct">direct</option>
          <option value="mapped">mapped</option>
          <option value="range">range</option>
        </select>
      </div>
      <div>
        <label className="block text-xs text-slate-500 mb-1">value_mapping (JSON {`{`}값:코드{`}`})</label>
        <input value={mapping} onChange={(e) => setMapping(e.target.value)} placeholder={type === "direct" ? "direct는 불필요" : '{"서울":"101000"}'} disabled={type === "direct"}
          className="w-72 px-3 py-2 border border-slate-300 rounded-lg text-sm disabled:bg-slate-100" />
      </div>
      <div>
        <label className="block text-xs text-slate-500 mb-1">순서</label>
        <input type="number" value={order} onChange={(e) => setOrder(Number(e.target.value))} className="w-20 px-3 py-2 border border-slate-300 rounded-lg text-sm" />
      </div>
      <button onClick={add} className="px-3 py-2 bg-blue-600 text-white rounded-lg text-sm flex items-center gap-1"><Plus size={16} /> 추가</button>
    </div>
  );

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<Partial<SearchMapping>>({});

  const startEdit = (m: SearchMapping) => {
    setEditingId(m.id);
    setDraft({ urlParamName: m.urlParamName, valueType: m.valueType, valueMapping: m.valueMapping ?? "", isEnabled: m.isEnabled, displayOrder: m.displayOrder });
  };

  const save = () => {
    if (editingId == null) return;
    if (!draft.urlParamName?.trim()) return;
    updateMutation.mutate({ id: editingId, data: draft as Partial<SearchMapping> });
    setEditingId(null);
  };

  return (
    <div>
      <div className="flex gap-2 mb-4 flex-wrap">
        {sites?.map((s) => (
          <button key={s.id} onClick={() => { setSite(s.siteName); setEditingId(null); }}
            className={`px-4 py-2 rounded-lg text-sm font-medium ${site === s.siteName ? "bg-blue-600 text-white" : "bg-white border border-slate-300 text-slate-600 hover:bg-slate-100"}`}>
            {s.displayName}
          </button>
        ))}
      </div>

      {addForm}

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">키</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">URL 파라미터</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">타입</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">value_mapping</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">순서</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">활성</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400">로딩 중...</td></tr>
            ) : !mappings?.length ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400">매핑이 없습니다</td></tr>
            ) : (
              mappings.map((m) =>
                editingId === m.id ? (
                  <tr key={m.id} className="bg-blue-50/50">
                    <td className="px-4 py-3 text-sm font-medium text-slate-800">{m.standardKey}</td>
                    <td className="px-4 py-3">
                      <input value={draft.urlParamName ?? ""} onChange={(e) => setDraft((d) => ({ ...d, urlParamName: e.target.value }))}
                        className="px-2 py-1 border border-slate-300 rounded-md text-sm" />
                    </td>
                    <td className="px-4 py-3">
                      <select value={draft.valueType ?? "mapped"} onChange={(e) => setDraft((d) => ({ ...d, valueType: e.target.value as ValueType }))}
                        className="px-2 py-1 border border-slate-300 rounded-md text-sm bg-white">
                        <option value="direct">direct</option>
                        <option value="mapped">mapped</option>
                        <option value="range">range</option>
                      </select>
                    </td>
                    <td className="px-4 py-3">
                      <input value={draft.valueMapping ?? (draft.valueType === "direct" ? "" : "{}")}
                        onChange={(e) => setDraft((d) => ({ ...d, valueMapping: e.target.value }))}
                        disabled={draft.valueType === "direct"}
                        className="w-full min-w-56 px-2 py-1 border border-slate-300 rounded-md text-xs font-mono disabled:bg-slate-100" />
                    </td>
                    <td className="px-4 py-3">
                      <input type="number" value={draft.displayOrder ?? 0} onChange={(e) => setDraft((d) => ({ ...d, displayOrder: Number(e.target.value) }))}
                        className="w-16 px-2 py-1 border border-slate-300 rounded-md text-sm" />
                    </td>
                    <td className="px-4 py-3">
                      <input type="checkbox" checked={!!draft.isEnabled} onChange={(e) => setDraft((d) => ({ ...d, isEnabled: e.target.checked }))} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button onClick={save} className="text-green-600 hover:text-green-800 p-1"><Check size={16} /></button>
                        <button onClick={() => setEditingId(null)} className="text-slate-400 hover:text-slate-600 p-1"><X size={16} /></button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  <tr key={m.id} className="hover:bg-slate-50 align-top">
                    <td className="px-4 py-3 text-sm font-medium text-slate-800">{m.standardKey}</td>
                    <td className="px-4 py-3 text-sm text-slate-600">{m.urlParamName}</td>
                    <td className="px-4 py-3"><span className="text-xs px-2 py-1 rounded-full bg-slate-100 text-slate-600">{m.valueType}</span></td>
                    <td className="px-4 py-3 text-xs font-mono text-slate-500 break-all">{m.valueMapping || "-"}</td>
                    <td className="px-4 py-3 text-sm text-slate-500">{m.displayOrder}</td>
                    <td className="px-4 py-3 text-sm">{m.isEnabled ? <span className="text-green-600">활성</span> : <span className="text-slate-400">비활성</span>}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        <button onClick={() => startEdit(m)} className="text-blue-500 hover:text-blue-700 p-1"><Pencil size={16} /></button>
                        <button onClick={async () => { if (await confirm(`${m.siteDisplayName} - ${m.standardKey} 매핑을 삭제하시겠습니까?`)) deleteMutation.mutate(m.id); }}
                          className="text-red-500 hover:text-red-700 p-1"><Trash2 size={16} /></button>
                      </div>
                    </td>
                  </tr>
                )
              )
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}