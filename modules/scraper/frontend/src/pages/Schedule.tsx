import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchCrawlers, executeCrawler } from "../api/scraper";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700" },
];

const CAREERS = ["경력무관", "1~3년", "3~5년", "5~10년", "10년 이상"];
const LOCATIONS = ["서울", "경기", "인천", "부산", "대구", "기타"];
const DAYS = [
  { id: 1, name: "월" }, { id: 2, name: "화" }, { id: 3, name: "수" },
  { id: 4, name: "목" }, { id: 5, name: "금" }, { id: 6, name: "토" }, { id: 0, name: "일" },
];

function toCron(hour: number, minute: number, days: number[]): string {
  if (days.length === 7) return `${minute} ${hour} * * *`;
  if (days.length === 5 && days.includes(1) && days.includes(5)) {
    return `${minute} ${hour} * * 1-5`;
  }
  return `${minute} ${hour} * * ${days.join(",")}`;
}

function parseCronToHuman(cron: string): string {
  const parts = cron.split(" ");
  if (parts.length < 5) return cron;
  const [min, hour, , , dayOfWeek] = parts;
  const days = dayOfWeek.split(",").map(d => {
    if (d === "1-5") return "평일";
    return DAYS.find(day => day.id === parseInt(d))?.name || d;
  }).join(", ");
  return `${hour}시 ${min}분 (${days})`;
}

export default function Schedule() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const state = location.state as any;

  const [name, setName] = useState("");
  const [keyword, setKeyword] = useState("");
  const [career, setCareer] = useState("3~5년");
  const [loc, setLoc] = useState("서울");
  const [selectedSites, setSelectedSites] = useState<string[]>(["saramin", "jobkorea", "wanted", "remember"]);
  const [hour, setHour] = useState(9);
  const [minute, setMinute] = useState(0);
  const [selectedDays, setSelectedDays] = useState<number[]>([1, 2, 3, 4, 5]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    if (state) {
      setKeyword(state.keyword || "");
      setCareer(state.career || "3~5년");
      setLoc(state.location || "서울");
      setSelectedSites(state.sites || ["saramin", "jobkorea", "wanted", "remember"]);
      setShowForm(true);
    }
  }, [state]);

  const { data: crawlers } = useQuery({
    queryKey: ["crawlers"],
    queryFn: fetchCrawlers,
  });

  const executeMutation = useMutation({
    mutationFn: executeCrawler,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["crawlers"] });
      alert("크롤러 실행이 시작되었습니다");
    },
  });

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

  const cronStr = toCron(hour, minute, selectedDays);

  const handleSave = () => {
    if (!name || !keyword) {
      alert("이름과 키워드를 입력하세요");
      return;
    }
    const config = {
      name, keyword, career, location: loc,
      sites: selectedSites,
      schedule: cronStr,
      hour, minute, days: selectedDays,
      createdAt: new Date().toISOString(),
    };
    localStorage.setItem(`schedule_${name}`, JSON.stringify(config));
    setShowForm(false);
    setEditingId(null);
    setName("");
    setKeyword("");
  };

  const handleEdit = (schedule: any) => {
    setName(schedule.name);
    setKeyword(schedule.keyword);
    setCareer(schedule.career);
    setLoc(schedule.location);
    setSelectedSites(schedule.sites);
    setHour(schedule.hour);
    setMinute(schedule.minute);
    setSelectedDays(schedule.days);
    setEditingId(schedule.name);
    setShowForm(true);
  };

  const handleDelete = (name: string) => {
    if (confirm(`"${name}" 스케줄을 삭제하시겠습니까?`)) {
      localStorage.removeItem(`schedule_${name}`);
    }
  };

  return (
    <div className="p-6 max-w-5xl mx-auto overflow-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold">📅 스케줄 관리</h1>
        {!showForm && (
          <button
            onClick={() => {
              setShowForm(true);
              setEditingId(null);
              setName("");
              setKeyword("");
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700"
          >
            + 신규 등록
          </button>
        )}
      </div>

      {showForm && (
        <div className="bg-white border border-slate-200 rounded-xl p-6 mb-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-4">
            {editingId ? "스케줄 수정" : "신규 스케줄 등록"}
          </h3>
          
          <div className="grid grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="block text-xs text-slate-500 mb-1">스케줄 이름</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: java_daily"
                  disabled={!!editingId}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-slate-50"
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

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-500 mb-1">경력</label>
                  <select
                    value={career}
                    onChange={(e) => setCareer(e.target.value)}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  >
                    {CAREERS.map((c) => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-slate-500 mb-1">지역</label>
                  <select
                    value={loc}
                    onChange={(e) => setLoc(e.target.value)}
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  >
                    {LOCATIONS.map((l) => <option key={l} value={l}>{l}</option>)}
                  </select>
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
                <label className="block text-xs text-slate-500 mb-2">실행 시간</label>
                <div className="flex items-center gap-3">
                  <select
                    value={hour}
                    onChange={(e) => setHour(Number(e.target.value))}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  >
                    {Array.from({ length: 24 }, (_, i) => (
                      <option key={i} value={i}>{String(i).padStart(2, "0")}시</option>
                    ))}
                  </select>
                  <span className="text-slate-400">:</span>
                  <select
                    value={minute}
                    onChange={(e) => setMinute(Number(e.target.value))}
                    className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  >
                    {[0, 10, 15, 30, 45].map((m) => (
                      <option key={m} value={m}>{String(m).padStart(2, "0")}분</option>
                    ))}
                  </select>
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
                  미리보기: {cronStr}
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  onClick={handleSave}
                  className="px-5 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700"
                >
                  {editingId ? "수정" : "저장"}
                </button>
                <button
                  onClick={() => { setShowForm(false); setEditingId(null); }}
                  className="px-5 py-2.5 bg-slate-100 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-200"
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div>
        <h2 className="text-lg font-bold mb-4">등록된 스케줄</h2>
        
        {crawlers && crawlers.length > 0 ? (
          <div className="space-y-4">
            {crawlers.map((c) => (
              <div
                key={c.id}
                className="bg-white border border-slate-200 rounded-xl p-5 hover:border-slate-300 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-4">
                    <span className="text-3xl">🤖</span>
                    <div>
                      <div className="font-bold text-lg">{c.name}</div>
                      <div className="text-sm text-slate-500 mt-0.5">
                        스케줄: {parseCronToHuman(c.schedule)}
                      </div>
                      <div className="text-sm text-slate-500">
                        경로: {c.localPath}
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => executeMutation.mutate(c.id)}
                      disabled={executeMutation.isPending}
                      className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 disabled:opacity-50"
                    >
                      ▶ 수동실행
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
                      onClick={() => handleDelete(c.name)}
                      className="px-3 py-2 bg-red-50 text-red-600 rounded-lg text-sm hover:bg-red-100"
                    >
                      🗑️
                    </button>
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2">
                  {c.siteConfigs?.map((sc: any) => {
                    const siteInfo = SITES.find((s) => s.id === sc.siteName);
                    return (
                      <div key={sc.siteName} className="bg-slate-50 rounded-lg px-3 py-2 text-sm">
                        <span className={`px-2 py-0.5 rounded text-xs font-medium ${siteInfo?.color || "bg-slate-100"}`}>
                          {sc.displayName}
                        </span>
                        <span className="ml-2 text-slate-600">
                          {sc.paramValues?.keyword || "-"} | {sc.paramValues?.career || "-"} | {sc.paramValues?.location || "-"}
                        </span>
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
