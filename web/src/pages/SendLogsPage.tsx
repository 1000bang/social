import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Pagination } from "../components/Pagination";
import { SimpleBarChart } from "../components/SimpleBarChart";
import type { ChartBucket, ChartGranularity, SendLogResponse, SendLogSummaryResponse } from "../api/types";

const GRANULARITY_LABEL: Record<ChartGranularity, string> = {
	HOUR: "시간별",
	DAY: "일별",
	MONTH: "월별",
};

export function SendLogsPage() {
	const [logs, setLogs] = useState<SendLogResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);

	const [summary, setSummary] = useState<SendLogSummaryResponse | null>(null);

	const [granularity, setGranularity] = useState<ChartGranularity>("DAY");
	const [chartData, setChartData] = useState<ChartBucket[]>([]);
	const [chartLoading, setChartLoading] = useState(true);

	useEffect(() => {
		setLoading(true);
		api
			.listSendLogs(page)
			.then((res) => {
				setLogs(res.content);
				setTotalPages(res.totalPages);
			})
			.catch((err) => setError(err instanceof Error ? err.message : "발송 통계를 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	}, [page]);

	useEffect(() => {
		api.getSendLogSummary().then(setSummary).catch(() => setSummary(null));
	}, []);

	useEffect(() => {
		setChartLoading(true);
		api
			.getSendLogChart(granularity)
			.then(setChartData)
			.catch(() => setChartData([]))
			.finally(() => setChartLoading(false));
	}, [granularity]);

	return (
		<div>
			<h2>발송 통계</h2>

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
					<select value={granularity} onChange={(e) => setGranularity(e.target.value as ChartGranularity)}>
						<option value="HOUR">시간별</option>
						<option value="DAY">일별</option>
						<option value="MONTH">월별</option>
					</select>
				</div>
				{chartLoading && <p>불러오는 중...</p>}
				{!chartLoading && chartData.length === 0 && <p className="hint">{GRANULARITY_LABEL[granularity]} 발송 데이터가 없습니다.</p>}
				{!chartLoading && chartData.length > 0 && <SimpleBarChart data={chartData} />}
			</div>

			{loading && <p>불러오는 중...</p>}
			{error && <p className="error">{error}</p>}
			{!loading && !error && logs.length === 0 && <p>아직 발송 이력이 없습니다.</p>}
			{logs.length > 0 && (
				<table className="data-table">
					<thead>
						<tr>
							<th>템플릿</th>
							<th>대상</th>
							<th>수신자</th>
							<th>결과</th>
							<th>실패 사유</th>
							<th>일시</th>
						</tr>
					</thead>
					<tbody>
						{logs.map((log) => (
							<tr key={log.id}>
								<td>{log.templateName}</td>
								<td>{log.audienceType ?? "-"}</td>
								<td>{log.recipientUsername ? `@${log.recipientUsername}` : log.recipientPlatformUserId}</td>
								<td className={log.result === "SUCCESS" ? "success" : "failure"}>{log.result}</td>
								<td>{log.failureReason ?? "-"}</td>
								<td>{new Date(log.createdAt).toLocaleString("ko-KR")}</td>
							</tr>
						))}
					</tbody>
				</table>
			)}
			<Pagination page={page} totalPages={totalPages} onChange={setPage} />
		</div>
	);
}
