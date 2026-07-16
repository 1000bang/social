import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Pagination } from "../components/Pagination";
import { SimpleBarChart } from "../components/SimpleBarChart";
import type {
	AudienceType,
	ChartBucket,
	ChartGranularity,
	SendLogResponse,
	SendLogSummaryResponse,
	TemplateRankingResponse,
} from "../api/types";

const INSIGHT_DISPLAY_COUNT = 2;

function pickRandom<T>(items: T[], count: number): T[] {
	const shuffled = [...items];
	for (let i = shuffled.length - 1; i > 0; i--) {
		const j = Math.floor(Math.random() * (i + 1));
		[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
	}
	return shuffled.slice(0, count);
}

const GRANULARITY_LABEL: Record<ChartGranularity, string> = {
	HOUR: "시간별",
	DAY: "일별",
	MONTH: "월별",
};

const MAX_DAY_RANGE = 10;
const MAX_LOG_SEARCH_DAYS = 31;

function toDateStr(d: Date): string {
	// toISOString()은 UTC 기준이라 자정 전후로 로컬 날짜와 하루 어긋날 수 있어, 로컬 날짜 요소를 직접 조합한다.
	const year = d.getFullYear();
	const month = String(d.getMonth() + 1).padStart(2, "0");
	const day = String(d.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

function todayStr(): string {
	return toDateStr(new Date());
}

function addDays(dateStr: string, days: number): string {
	const d = new Date(`${dateStr}T00:00:00`);
	d.setDate(d.getDate() + days);
	return toDateStr(d);
}

function daysBetween(a: string, b: string): number {
	const d1 = new Date(`${a}T00:00:00`);
	const d2 = new Date(`${b}T00:00:00`);
	return Math.round((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
}

function formatLogDate(iso: string): string {
	const date = new Date(iso);
	const now = new Date();
	const isToday =
		date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate();

	if (isToday) {
		return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
	}

	const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
	const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
	const diffDays = Math.round((startOfToday.getTime() - startOfDate.getTime()) / (1000 * 60 * 60 * 24));
	return `${diffDays}일전`;
}

export function SendLogsPage() {
	const [logs, setLogs] = useState<SendLogResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);

	const [summary, setSummary] = useState<SendLogSummaryResponse | null>(null);
	const [insights, setInsights] = useState<string[]>([]);

	const [granularity, setGranularity] = useState<ChartGranularity>("DAY");
	const [hourDate, setHourDate] = useState(todayStr());
	const [dayFrom, setDayFrom] = useState(addDays(todayStr(), -7));
	const [dayTo, setDayTo] = useState(todayStr());
	const [monthYear, setMonthYear] = useState(new Date().getFullYear());
	const [chartData, setChartData] = useState<ChartBucket[]>([]);
	const [chartLoading, setChartLoading] = useState(true);

	const handleDayFromChange = (value: string) => {
		setDayFrom(value);
		if (daysBetween(value, dayTo) < 0) {
			setDayTo(value);
		} else if (daysBetween(value, dayTo) > MAX_DAY_RANGE - 1) {
			setDayTo(addDays(value, MAX_DAY_RANGE - 1));
		}
	};

	const handleDayToChange = (value: string) => {
		setDayTo(value);
		if (daysBetween(dayFrom, value) < 0) {
			setDayFrom(value);
		} else if (daysBetween(dayFrom, value) > MAX_DAY_RANGE - 1) {
			setDayFrom(addDays(value, -(MAX_DAY_RANGE - 1)));
		}
	};

	const [topTemplates, setTopTemplates] = useState<TemplateRankingResponse[]>([]);
	const [topTemplatesLoading, setTopTemplatesLoading] = useState(true);

	const [filtersOpen, setFiltersOpen] = useState(false);
	const [templateNameInput, setTemplateNameInput] = useState("");
	const [audienceTypeInput, setAudienceTypeInput] = useState<"" | AudienceType>("");
	const [logFrom, setLogFrom] = useState("");
	const [logTo, setLogTo] = useState("");
	const [logFromText, setLogFromText] = useState("");
	const [logToText, setLogToText] = useState("");
	const [appliedFilters, setAppliedFilters] = useState<{
		templateName?: string;
		audienceType?: AudienceType;
		from?: string;
		to?: string;
	}>({});

	const isValidDateStr = (value: string): boolean => {
		if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
		const d = new Date(`${value}T00:00:00`);
		return !isNaN(d.getTime()) && toDateStr(d) === value;
	};

	const handleLogFromTextChange = (value: string) => {
		setLogFromText(value);
		if (!isValidDateStr(value) || value > todayStr()) return;
		setLogFrom(value);
		if (!logTo) return;
		if (daysBetween(value, logTo) < 0) {
			setLogTo(value);
			setLogToText(value);
		} else if (daysBetween(value, logTo) > MAX_LOG_SEARCH_DAYS - 1) {
			const clamped = addDays(value, MAX_LOG_SEARCH_DAYS - 1);
			setLogTo(clamped);
			setLogToText(clamped);
		}
	};

	const handleLogToTextChange = (value: string) => {
		setLogToText(value);
		if (!isValidDateStr(value) || value > todayStr()) return;
		setLogTo(value);
		if (!logFrom) return;
		if (daysBetween(logFrom, value) < 0) {
			setLogFrom(value);
			setLogFromText(value);
		} else if (daysBetween(logFrom, value) > MAX_LOG_SEARCH_DAYS - 1) {
			const clamped = addDays(value, -(MAX_LOG_SEARCH_DAYS - 1));
			setLogFrom(clamped);
			setLogFromText(clamped);
		}
	};

	const handleSearch = () => {
		setPage(0);
		setAppliedFilters({
			templateName: templateNameInput.trim() || undefined,
			audienceType: audienceTypeInput || undefined,
			from: logFrom || undefined,
			to: logTo || undefined,
		});
	};

	const handleResetFilters = () => {
		setTemplateNameInput("");
		setAudienceTypeInput("");
		setLogFrom("");
		setLogTo("");
		setLogFromText("");
		setLogToText("");
		setPage(0);
		setAppliedFilters({});
	};

	useEffect(() => {
		setLoading(true);
		api
			.listSendLogs(page, 10, appliedFilters)
			.then((res) => {
				setLogs(res.content);
				setTotalPages(res.totalPages);
			})
			.catch((err) => setError(err instanceof Error ? err.message : "발송 통계를 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	}, [page, appliedFilters]);

	useEffect(() => {
		api.getSendLogSummary().then(setSummary).catch(() => setSummary(null));
	}, []);

	useEffect(() => {
		api
			.getSendLogInsights()
			.then((res) => setInsights(pickRandom(res.map((i) => i.text), INSIGHT_DISPLAY_COUNT)))
			.catch(() => setInsights([]));
	}, []);

	useEffect(() => {
		api
			.getTopTemplates()
			.then(setTopTemplates)
			.catch(() => setTopTemplates([]))
			.finally(() => setTopTemplatesLoading(false));
	}, []);

	useEffect(() => {
		setChartLoading(true);
		const params =
			granularity === "HOUR"
				? { date: hourDate }
				: granularity === "DAY"
					? { from: dayFrom, to: dayTo }
					: { year: monthYear };
		api
			.getSendLogChart(granularity, params)
			.then(setChartData)
			.catch(() => setChartData([]))
			.finally(() => setChartLoading(false));
	}, [granularity, hourDate, dayFrom, dayTo, monthYear]);

	return (
		<div>
			<h2>발송 통계</h2>

			{insights.length > 0 && (
				<div className="insight-list">
					{insights.map((text) => (
						<p className="insight-item" key={text}>
							{text}
						</p>
					))}
				</div>
			)}

			{summary && (
				<div className="summary-cards">
					<div className="summary-card">
						<div className="value">{summary.contactedUsersThisMonth}</div>
						<div className="label">이번 달 컨택한 사용자 수</div>
					</div>
					<div className="summary-card">
						<div className="value">{summary.messagesSentThisMonth}</div>
						<div className="label">이번 달 발송한 메시지 수 (팔로우 확인 메시지 제외)</div>
					</div>
				</div>
			)}

			<div className="chart-section">
				<div className="chart-header">
					<strong>발송 추이</strong>
					<select
						className="chart-field"
						value={granularity}
						onChange={(e) => setGranularity(e.target.value as ChartGranularity)}
					>
						<option value="HOUR">시간별</option>
						<option value="DAY">일별</option>
						<option value="MONTH">월별</option>
					</select>
				</div>
				<div className="chart-controls">
					{granularity === "HOUR" && (
						<input
							className="chart-field"
							type="date"
							value={hourDate}
							max={todayStr()}
							onChange={(e) => setHourDate(e.target.value)}
						/>
					)}
					{granularity === "DAY" && (
						<>
							<input
								className="chart-field"
								type="date"
								value={dayFrom}
								max={todayStr()}
								onChange={(e) => handleDayFromChange(e.target.value)}
							/>
							<span>~</span>
							<input
								className="chart-field"
								type="date"
								value={dayTo}
								max={todayStr()}
								onChange={(e) => handleDayToChange(e.target.value)}
							/>
						</>
					)}
					{granularity === "MONTH" && (
						<select className="chart-field" value={monthYear} onChange={(e) => setMonthYear(Number(e.target.value))}>
							{Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i).map((y) => (
								<option key={y} value={y}>
									{y}년
								</option>
							))}
						</select>
					)}
				</div>
				{chartLoading && <p>불러오는 중...</p>}
				{!chartLoading && chartData.length === 0 && <p className="hint">{GRANULARITY_LABEL[granularity]} 발송 데이터가 없습니다.</p>}
				{!chartLoading && chartData.length > 0 && (
					<SimpleBarChart data={chartData} scrollable={granularity === "HOUR"} />
				)}
			</div>

			<div className="chart-section">
				<div className="chart-header">
					<strong>템플릿 TOP 10 랭크</strong>
				</div>
				{topTemplatesLoading && <p>불러오는 중...</p>}
				{!topTemplatesLoading && topTemplates.length === 0 && <p className="hint">아직 발송 이력이 없습니다.</p>}
				{topTemplates.length > 0 && (
					<table className="data-table">
						<thead>
							<tr>
								<th>순번</th>
								<th>템플릿명</th>
								<th>컨택 사용자 수</th>
								<th>메시지 발송수</th>
							</tr>
						</thead>
						<tbody>
							{topTemplates.map((t, index) => (
								<tr key={t.templateId}>
									<td>{index + 1}</td>
									<td>{t.templateName}</td>
									<td>{t.contactedUsers}</td>
									<td>{t.messagesSent}</td>
								</tr>
							))}
						</tbody>
					</table>
				)}
			</div>

			<div className="chart-section">
				<div className="chart-header">
					<strong>발송 로그</strong>
					<button type="button" onClick={() => setFiltersOpen((v) => !v)}>
						검색조건 {filtersOpen ? "닫기" : "펼치기"}
					</button>
				</div>
				{filtersOpen && (
					<div className="log-filters">
						<label>
							템플릿명
							<input
								value={templateNameInput}
								onChange={(e) => setTemplateNameInput(e.target.value)}
								placeholder="템플릿명으로 검색"
							/>
						</label>
						<label>
							대상
							<select
								value={audienceTypeInput}
								onChange={(e) => setAudienceTypeInput(e.target.value as "" | AudienceType)}
							>
								<option value="">전체</option>
								<option value="FOLLOWER">팔로워</option>
								<option value="NON_FOLLOWER">논팔로워</option>
							</select>
						</label>
						<label>
							날짜 (최대 {MAX_LOG_SEARCH_DAYS}일)
							<div className="log-filter-date-range">
								<input
									type="text"
									placeholder="YYYY-MM-DD"
									maxLength={10}
									value={logFromText}
									onChange={(e) => handleLogFromTextChange(e.target.value)}
								/>
								<span>~</span>
								<input
									type="text"
									placeholder="YYYY-MM-DD"
									maxLength={10}
									value={logToText}
									onChange={(e) => handleLogToTextChange(e.target.value)}
								/>
							</div>
						</label>
						<div className="log-filter-actions">
							<button type="button" className="primary-button" onClick={handleSearch}>
								검색
							</button>
							<button type="button" onClick={handleResetFilters}>
								초기화
							</button>
						</div>
					</div>
				)}

				{loading && <p>불러오는 중...</p>}
				{error && <p className="error">{error}</p>}
				{!loading && !error && logs.length === 0 && <p>아직 발송 이력이 없습니다.</p>}
				{logs.length > 0 && (
					<table className="data-table">
						<thead>
							<tr>
								<th>템플릿</th>
								<th className="hide-mobile">대상</th>
								<th className="hide-mobile">수신자</th>
								<th>결과</th>
								<th className="hide-mobile">실패 사유</th>
								<th>일시</th>
							</tr>
						</thead>
						<tbody>
							{logs.map((log) => (
								<tr key={log.id}>
									<td>{log.templateName}</td>
									<td className="hide-mobile">{log.audienceType ?? "-"}</td>
									<td className="hide-mobile">{log.recipientUsername ? `@${log.recipientUsername}` : log.recipientPlatformUserId}</td>
									<td className={log.result === "SUCCESS" ? "success" : "failure"}>{log.result}</td>
									<td className="hide-mobile">{log.failureReason ?? "-"}</td>
									<td>{formatLogDate(log.createdAt)}</td>
								</tr>
							))}
						</tbody>
					</table>
				)}
				<Pagination page={page} totalPages={totalPages} onChange={setPage} />
			</div>
		</div>
	);
}
