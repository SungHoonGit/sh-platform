import { useState, useEffect, useMemo, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../hooks/useAuth";
import { usePushNotification } from "../hooks/usePushNotification";
import {
  fetchCrawlers,
  executeCrawler,
  fetchSites,
  saveCrawler,
  updateCrawler,
  deleteCrawler,
  saveSiteConfig,
} from "../api/scraper";
import {
  REGIONS,
  DEFAULT_LOCATIONS,
  CAREER_TOTAL,
  isCareerActive,
  CareerRangeSlider,
  LocationMultiSelect,
} from "../components/SearchFilters";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";

const DEFAULT_SITES = ["saramin", "jobkorea"];

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700 border-blue-200" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700 border-green-200" },
];

const DAYS = [
  { id: 1, name: "월" }, { id: 2, name: "화" }, { id: 3, name: "수" },
  { id: 4, name: "목" }, { id: 5, name: "금" }, { id: 6, name: "토" }, { id: 0, name: "일" },
];

const MINUTES = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55];

const ICON_OPTIONS = ["🤖", "💼", "🚀", "📋", "🔍", "📊", "🎯", "💻", "🔧", "📱"];

interface TimePair {
  hour: number;
  minute: number;
}

function formatHour(h: number): string {
  if (h === 0) return "오전 12";
  if (h < 12) return `오전 ${h}`;
  if (h === 12) return "오후 12";
  return `오후 ${h - 12}`;
}

function formatTimePair(tp: TimePair): string {
  return `${formatHour(tp.hour)}:${String(tp.minute).padStart(2, "0")}`;
}

function legacyCareerRange(career: string): [number, number] {
  switch (career) {
    case "1~3년": return [1, 3];
    case "3~5년": return [3, 5];
    case "5~10년": return [5, 10];
    case "10년이상": case "10년 이상": return [10, CAREER_TOTAL];
    default: return [0, CAREER_TOTAL];
  }
}

function timePairsToCron(timePairs: TimePair[], days: number[]): string {
  if (timePairs.length === 0) return "0 9 * * *";

  const dow = days.length === 7 ? "*"
    : days.length === 5 && days.includes(1) && days.includes(5) ? "1-5"
    : days.join(",");

  const lines = timePairs
    .map(tp => `${tp.minute} ${tp.hour} * * ${dow}`)
    .join("\n");

  return lines;
}

function parseCronToHuman(cron: string): string {
  const lines = cron.split("\n").filter(l => l.trim());
  if (lines.length === 0) return cron;

  const times = lines.map(line => {
    const parts = line.trim().split(" ");
    if (parts.length < 5) return line;
    const [min, hour] = parts;
    return `${formatHour(parseInt(hour, 10))}:${min.padStart(2, "0")}`;
  });

  const dow = lines[0].split(" ")[4] || "*";
  const days = dow.split(",").map(d => {
    if (d === "*") return "매일";
    if (d === "1-5") return "평일";
    return DAYS.find(day => day.id === parseInt(d))?.name || d;
  }).join(", ");

  return `${times.join(", ")} (${days})`;
}

function parseCronToSchedule(cron: string): { timePairs: TimePair[]; days: number[] } {
  const lines = cron.split("\n").filter(l => l.trim());
  if (lines.length === 0) return { timePairs: [{ hour: 9, minute: 0 }], days: [1, 2, 3, 4, 5] };

  const timePairs: TimePair[] = lines.map(line => {
    const parts = line.trim().split(" ");
    if (parts.length < 5) return { hour: 9, minute: 0 };
    return {
      minute: parseInt(parts[0], 10) || 0,
      hour: parseInt(parts[1], 10) || 9,
    };
  });

  const dow = lines[0].split(" ")[4] || "*";
  let days: number[];
  if (dow === "*") days = [0, 1, 2, 3, 4, 5, 6];
  else if (dow === "1-5") days = [1, 2, 3, 4, 5];
  else days = dow.split(",").map((d) => parseInt(d, 10)).filter((n) => !isNaN(n));

  return { timePairs, days };
}

export default function Schedule() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const state = location.state as any;
  const { startProgress } = useCrawlProgress();
  const { user } = useAuth();

  const [name, setName] = useState("");
  const [keyword, setKeyword] = useState("");
  const [careerMin, setCareerMin] = useState(0);
  const [careerMax, setCareerMax] = useState(CAREER_TOTAL);
  const [locations, setLocations] = useState<string[]>(DEFAULT_LOCATIONS);
  const [selectedSites, setSelectedSites] = useState<string[]>(DEFAULT_SITES);
  const [timePairs, setTimePairs] = useState<TimePair[]>([{ hour: 9, minute: 0 }]);
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4, 5]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [scheduleIcon, setScheduleIcon] = useState("🤖");
  const [emailNotification, setEmailNotification] = useState(false);
  const [pushNotification, setPushNotification] = useState(false);
  const savingRef = useRef(false);
  const { isSupported: pushSupported, isSubscribed: pushSubscribed, subscribe: subscribePush, unsubscribe: unsubscribePush } = usePushNotification();

  useEffect(() => {
    if (state) {
      setKeyword(state.keyword || "");
      if (typeof state.career === "string") {
        const [min, max] = legacyCareerRange(state.career);
        setCareerMin(min);
        setCareerMax(max);
      } else {
        setCareerMin(state.careerMin ?? 0);
        setCareerMax(state.careerMax ?? CAREER_TOTAL);
      }
      if (typeof state.location === "string") {
        setLocations(state.location === "전체" ? DEFAULT_LOCATIONS : [state.location]);
      } else {
        setLocations(state.locations?.length ? state.locations : DEFAULT_LOCATIONS);
      }
      setSelectedSites(state.sites || DEFAULT_SITES);
      setEditingId(null);
      setShowForm(true);
    }
  }, [state]);

  const { data: crawlers, error: crawlersError } = useQuery({
    queryKey: ["crawlers"],
    queryFn: fetchCrawlers,
  });

  const { data: sites } = useQuery({
    queryKey: ["sites"],
    queryFn: fetchSites,
  });

  const siteMap = useMemo(() => {
    const m = new Map<string, number>();
    (sites ?? []).forEach((s) => m.set(s.siteName, s.id));
    return m;
  }, [sites]);

  const executeMutation = useMutation({
    mutationFn: async (configId: number) => {
      const crawler = crawlers?.find((c) => c.id === configId);
      startProgress(configId, crawler?.name || "공고 수집");
      await new Promise(resolve => setTimeout(resolve, 100));
      return executeCrawler(configId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["crawlers"] });
    },
    onError: (e: Error) => alert(`실행 실패: ${e.message}`),
  });

  const [runningIds, setRunningIds] = useState<Set<number>>(new Set());

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        name: name.trim(),
        description: keyword.trim(),
        schedule: cronStr,
        scheduleIcon: scheduleIcon,
        emailNotification: emailNotification,
        pushNotification: pushNotification,
        recipientEmail: emailNotification ? user?.email : null,
        isActive: true,
        retentionDays: 30,
      };
      let configId: number;
      if (editingId != null) {
        const updated = await updateCrawler(editingId, body);
        configId = updated.id;
      } else {
        const created = await saveCrawler(body);
        configId = created.id;
      }

      const paramValues = JSON.stringify({
        keyword: keyword.trim(),
        ...(isCareerActive(careerMin, careerMax)
          ? {
              ...(careerMin > 0 ? { careerMin: String(careerMin) } : {}),
              ...(careerMax < CAREER_TOTAL ? { careerMax: String(careerMax) } : {}),
            }
          : {}),
        ...(locations.length > 0 && locations.length < REGIONS.length
          ? { location: locations.join(",") }
          : {}),
      });

      const existing = crawlers?.find((c) => c.id === configId);
      const existingSites = existing?.siteConfigs ?? [];
      const allSites = new Set([...selectedSites, ...existingSites.map((sc) => sc.siteName)]);
      const results = await Promise.allSettled(
        [...allSites].map(async (siteName) => {
          const siteDefId = siteMap.get(siteName);
          if (siteDefId == null) return;
          const enabled = selectedSites.includes(siteName);
          const prev = existingSites.find((sc) => sc.siteName === siteName)?.paramValues;
          await saveSiteConfig(configId, siteDefId, {
            paramValues: enabled ? paramValues : prev || paramValues,
            isEnabled: enabled,
          });
        })
      );
      const failed = results.find((r) => r.status === "rejected") as
        | PromiseRejectedResult
        | undefined;
      if (failed) {
        throw new Error(`사이트 설정 저장 실패: ${(failed.reason as Error)?.message || "INTERNAL_ERROR"}`);
      }
      return configId;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["crawlers"] });
      alert("스케줄이 저장되었습니다");
      resetForm();
    },
    onError: (e: Error) => alert(`저장 실패: ${e.message}`),
    onSettled: () => {
      savingRef.current = false;
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCrawler,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["crawlers"] });
      alert("스케줄이 삭제되었습니다");
    },
    onError: (e: Error) => alert(`삭제 실패: ${e.message}`),
  });

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setName("");
    setKeyword("");
    setCareerMin(0);
    setCareerMax(CAREER_TOTAL);
    setLocations(DEFAULT_LOCATIONS);
    setSelectedSites(DEFAULT_SITES);
    setTimePairs([{ hour: 9, minute: 0 }]);
    setSelectedDays([1, 2, 3, 4, 5]);
    setScheduleIcon("🤖");
    setEmailNotification(false);
    setPushNotification(false);
  };

  const handleSave = () => {
    if (savingRef.current || saveMutation.isPending) return;
    if (!name.trim() || !keyword.trim()) {
      alert("이름과 키워드를 입력하세요");
      return;
    }
    const dup = (crawlers ?? []).some(
      (c) => c.id !== editingId && c.name.trim() === name.trim()
    );
    if (dup) {
      alert(`이미 같은 이름("${name.trim()}")의 스케줄이 있습니다`);
      return;
    }
    savingRef.current = true;
    saveMutation.mutate();
  };

  const toggleSite = (siteId: string) => {
    setSelectedSites((prev) =>
      prev.includes(siteId) ? prev.filter((s) => s !== siteId) : [...prev, siteId]
    );
  };

  const toggleDay = (dayId: number) => {
    setSelectedDays((prev) =>
      prev.includes(dayId) ? prev.filter((d) => d !== dayId) : [...prev, dayId].sort()
    );
  };

  const addTimePair = () => {
    if (timePairs.length >= 10) {
      alert("시간은 최대 10개까지 추가할 수 있습니다.");
      return;
    }
    setTimePairs(prev => [...prev, { hour: 9, minute: 0 }]);
  };

  const removeTimePair = (index: number) => {
    if (timePairs.length <= 1) return;
    setTimePairs(prev => prev.filter((_, i) => i !== index));
  };

  const updateTimePair = (index: number, field: "hour" | "minute", value: number) => {
    setTimePairs(prev => prev.map((tp, i) =>
      i === index ? { ...tp, [field]: value } : tp
    ));
  };

  const cronStr = timePairsToCron(timePairs, selectedDays);

  const handleEdit = (c: any) => {
    setName(c.name);
    setEditingId(c.id);
    setScheduleIcon(c.scheduleIcon || "🤖");
    setEmailNotification(c.emailNotification || false);
    setPushNotification(c.pushNotification || false);
    const scs = c.siteConfigs || [];
    const enabled = scs.filter((sc: any) => sc.isEnabled);
    const first = enabled[0] || scs[0];
    let params: any = {};
    try {
      params = first?.paramValues ? JSON.parse(first.paramValues) : {};
    } catch {
      params = {};
    }
    setKeyword(params.keyword || "");
    const cMin = params.careerMin != null ? parseInt(params.careerMin, 10) : null;
    const cMax = params.careerMax != null ? parseInt(params.careerMax, 10) : null;
    setCareerMin(cMin ?? 0);
    setCareerMax(cMax ?? CAREER_TOTAL);
    const locs = params.location
      ? params.location.split(",").map((s: string) => s.trim()).filter(Boolean)
      : [];
    setLocations(locs.length ? locs : DEFAULT_LOCATIONS);
    setSelectedSites(enabled.length ? enabled.map((sc: any) => sc.siteName) : DEFAULT_SITES);
    const parsed = parseCronToSchedule(c.schedule);
    setTimePairs(parsed.timePairs);
    setSelectedDays(parsed.days);
    setShowForm(true);
    setTimeout(() => {
      document.getElementById("schedule-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }, 100);
  };

  const handleDelete = async (c: any) => {
    if (!(await confirm(`"${c.name}" 스케줄을 삭제하시겠습니까?`))) return;
    deleteMutation.mutate(c.id);
  };

  return (
    <div className="p-6 max-w-5xl mx-auto overflow-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold">📅 스케줄 관리</h1>
        {!showForm && (
          <button
            onClick={() => {
              resetForm();
              setShowForm(true);
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700"
          >
            + 신규 등록
          </button>
        )}
      </div>

      {/* 스케줄 폼 - 하단에 별도 영역으로 표시 */}
      {showForm && (
        <div id="schedule-form" className="bg-white border-2 border-blue-200 rounded-xl p-6 mb-6 scroll-mt-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-slate-700">
              {editingId ? "📝 스케줄 수정" : "신규 스케줄 등록"}
            </h3>
            <button onClick={resetForm} className="text-slate-400 hover:text-slate-600">✕</button>
          </div>
          
          <div className="grid grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="block text-xs text-slate-500 mb-1">스케줄 이름</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: java_daily"
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-500 mb-1">키워드</label>
                <input
                  type="text"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  placeholder="React, Java..."
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-500 mb-1">아이콘</label>
                <div className="flex gap-2">
                  {ICON_OPTIONS.map((icon) => (
                    <button
                      key={icon}
                      type="button"
                      onClick={() => setScheduleIcon(icon)}
                      className={`w-10 h-10 rounded-lg text-xl flex items-center justify-center transition-colors ${
                        scheduleIcon === icon
                          ? "bg-blue-100 border-2 border-blue-500"
                          : "bg-slate-50 border border-slate-200 hover:bg-slate-100"
                      }`}
                    >
                      {icon}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={emailNotification}
                    onChange={(e) => setEmailNotification(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
                  />
                  <span className="text-xs text-slate-600">신규 공고 수집 시 이메일 알림</span>
                </label>
              </div>

              {pushSupported && (
                <div>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={pushNotification}
                      onChange={async (e) => {
                        if (e.target.checked) {
                          const ok = await subscribePush();
                          if (ok) {
                            setPushNotification(true);
                          } else {
                            alert("브라우저 알림 권한이 필요합니다.\n\n브라우저 주소창 왼쪽 🔒 아이콘 → 알림 → 허용으로 변경해주세요.");
                          }
                        } else {
                          await unsubscribePush();
                          setPushNotification(false);
                        }
                      }}
                      className="w-4 h-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
                    />
                    <span className="text-xs text-slate-600">브라우저 푸쉬 알림</span>
                    {!pushSubscribed && (
                      <span className="text-[10px] text-slate-400 ml-1" title="체크박스 클릭 시 브라우저에서 알림 권한 요청 팝업이 뜹니다">(권한 필요)</span>
                    )}
                    <span className="relative group ml-1">
                      <span className="inline-block w-3.5 h-3.5 rounded-full bg-slate-300 text-white text-[9px] font-bold leading-[14px] text-center cursor-help">?</span>
                      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 w-56 p-2 bg-slate-800 text-white text-[10px] leading-relaxed rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50">
                        푸쉬 알림이 오려면:<br/>
                        1. 브라우저 알림 권한 허용<br/>
                        2. Windows 알림 설정 켬<br/>
                        3. 브라우저가 켜져 있어야 함
                      </span>
                    </span>
                  </label>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-500 mb-1">경력</label>
                  <CareerRangeSlider
                    min={careerMin}
                    max={careerMax}
                    onMinChange={setCareerMin}
                    onMaxChange={setCareerMax}
                  />
                </div>
                <div>
                  <LocationMultiSelect
                    selected={locations}
                    onToggle={(loc) =>
                      setLocations((prev) =>
                        prev.includes(loc) ? prev.filter((l) => l !== loc) : [...prev, loc]
                      )
                    }
                    onSelectAll={() => setLocations([...REGIONS])}
                    onClear={() => setLocations([])}
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-500 mb-2">사이트</label>
                <div className="flex gap-2">
                  {SITES.map((site) => (
                    <label
                      key={site.id}
                      className={`flex items-center gap-1.5 px-3 py-2 rounded-lg border cursor-pointer text-xs ${
                        selectedSites.includes(site.id)
                          ? "border-blue-300 bg-blue-50"
                          : "border-slate-200"
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={selectedSites.includes(site.id)}
                        onChange={() => toggleSite(site.id)}
                        className="w-3.5 h-3.5"
                      />
                      {site.name}
                    </label>
                  ))}
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs text-slate-500">실행 시간</label>
                  <button
                    onClick={addTimePair}
                    className="text-xs text-blue-600 hover:text-blue-800"
                  >
                    + 시간 추가
                  </button>
                </div>

                <div className="space-y-2">
                  {timePairs.map((tp, index) => (
                    <div key={index} className="flex items-center gap-2 bg-slate-50 rounded-lg p-2">
                      <select
                        value={tp.hour}
                        onChange={(e) => updateTimePair(index, "hour", Number(e.target.value))}
                        className="px-2 py-1.5 border border-slate-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      >
                        {Array.from({ length: 24 }, (_, i) => (
                          <option key={i} value={i}>{formatHour(i)}</option>
                        ))}
                      </select>
                      <span className="text-slate-400">:</span>
                      <select
                        value={tp.minute}
                        onChange={(e) => updateTimePair(index, "minute", Number(e.target.value))}
                        className="px-2 py-1.5 border border-slate-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      >
                        {MINUTES.map((m) => (
                          <option key={m} value={m}>{String(m).padStart(2, "0")}분</option>
                        ))}
                      </select>
                      <button
                        onClick={() => removeTimePair(index)}
                        disabled={timePairs.length <= 1}
                        className="px-2 py-1 text-red-500 hover:text-red-700 disabled:opacity-30 disabled:cursor-not-allowed"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-500 mb-2">실행 요일</label>
                <div className="flex gap-2">
                  {DAYS.map((day) => (
                    <button
                      key={day.id}
                      onClick={() => toggleDay(day.id)}
                      className={`w-10 h-10 rounded-full text-sm font-medium transition-colors ${
                        selectedDays.includes(day.id)
                          ? "bg-blue-600 text-white"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {day.name}
                    </button>
                  ))}
                </div>
                <div className="mt-2 text-xs text-slate-500">
                  미리보기: {timePairs.map(tp => formatTimePair(tp)).join(", ")}
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  onClick={handleSave}
                  disabled={saveMutation.isPending || savingRef.current}
                  className="px-5 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {saveMutation.isPending || savingRef.current ? "저장 중..." : editingId ? "수정 완료" : "저장"}
                </button>
                <button
                  onClick={resetForm}
                  className="px-5 py-2.5 bg-slate-100 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-200"
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 등록된 스케줄 목록 */}
      <div>
        <h2 className="text-lg font-bold mb-4">등록된 스케줄</h2>

        {crawlersError ? (
          <div className="text-center py-12 text-red-500">
            스케줄 목록을 불러오지 못했습니다: {(crawlersError as Error).message}
          </div>
        ) : crawlers && crawlers.length > 0 ? (
          <div className="space-y-4">
            {crawlers.map((c) => (
              <div
                key={c.id}
                className="bg-white border border-slate-200 rounded-xl p-5 hover:border-slate-300 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-4">
                    <span className="text-3xl">{c.scheduleIcon || "🤖"}</span>
                    <div>
                      <div className="font-bold text-lg">{c.name}</div>
                      <div className="text-sm text-slate-500 mt-0.5">
                        스케줄: {parseCronToHuman(c.schedule)}
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        setRunningIds(prev => new Set(prev).add(c.id));
                        executeMutation.mutate(c.id, {
                          onSettled: () => {
                            setRunningIds(prev => {
                              const next = new Set(prev);
                              next.delete(c.id);
                              return next;
                            });
                          }
                        });
                      }}
                      disabled={runningIds.has(c.id)}
                      className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {runningIds.has(c.id) ? "⏳ 실행 중..." : "▶ 수동실행"}
                    </button>
                    <button
                      onClick={() => navigate(`/viewer?crawler=${c.id}`)}
                      className="px-4 py-2 bg-slate-100 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-200"
                    >
                      📁 결과보기
                    </button>
                    <button
                      onClick={() => handleEdit(c)}
                      className="px-3 py-2 bg-slate-100 text-slate-600 rounded-lg text-sm hover:bg-slate-200"
                    >
                      ✏️
                    </button>
                    <button
                      onClick={() => handleDelete(c)}
                      className="px-3 py-2 bg-red-50 text-red-600 rounded-lg text-sm hover:bg-red-100"
                    >
                      🗑️
                    </button>
                  </div>
                </div>

                {/* 사이트 정보 - 구분자 제거 */}
                <div className="mt-4 flex flex-wrap gap-2">
                  {c.siteConfigs?.map((sc: any) => {
                    const siteInfo = SITES.find((s) => s.id === sc.siteName);
                    return (
                      <div key={sc.siteName} className="bg-slate-50 rounded-lg px-3 py-2 text-sm">
                        <span className={`px-2 py-0.5 rounded text-xs font-medium ${siteInfo?.color || "bg-slate-100"}`}>
                          {sc.displayName}
                        </span>
                        {sc.paramValues?.keyword && (
                          <span className="ml-2 text-slate-600">{sc.paramValues.keyword}</span>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-slate-400">
            등록된 스케줄이 없습니다
          </div>
        )}
      </div>
    </div>
  );
}
