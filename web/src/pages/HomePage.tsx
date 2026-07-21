import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { RecoveryCardResponse, SendLogResponse, SendLogSummaryResponse, TemplateRankingResponse } from "../api/types";

const INSIGHT_DISPLAY_COUNT = 2;

function pickRandom<T>(items: T[], count: number): T[] {
	const shuffled = [...items];
	for (let i = shuffled.length - 1; i > 0; i--) {
		const j = Math.floor(Math.random() * (i + 1));
		[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
	}
	return shuffled.slice(0, count);
}

const QUICK_LINKS = [
	{ to: "/templates", label: "템플릿 관리", desc: "댓글/DM 키워드 자동 응답 템플릿을 만들고 관리해요" },
	{ to: "/send-logs", label: "발송 통계", desc: "발송 추이와 템플릿별 성과를 확인해요" },
	{ to: "/recovery", label: "미처리 대응", desc: "놓친 댓글에 대한 응답을 확인하고 처리해요" },
	{ to: "/settings", label: "환경설정", desc: "기본 응답 문구와 발송 옵션을 설정해요" },
];

export function HomePage() {
	const { me } = useAuth();
	const [summary, setSummary] = useState<SendLogSummaryResponse | null>(null);
	const [insights, setInsights] = useState<string[]>([]);
	const [topTemplates, setTopTemplates] = useState<TemplateRankingResponse[]>([]);
	const [recentLogs, setRecentLogs] = useState<SendLogResponse[]>([]);
	const [templateCount, setTemplateCount] = useState<number | null>(null);
	const [unprocessedCount, setUnprocessedCount] = useState<number | null>(null);

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
			.then((res) => setTopTemplates(res.slice(0, 3)))
			.catch(() => setTopTemplates([]));
	}, []);

	useEffect(() => {
		api
			.listSendLogs(0, 5)
			.then((res) => setRecentLogs(res.content))
			.catch(() => setRecentLogs([]));
	}, []);

	useEffect(() => {
		api
			.listTemplates(0, 1)
			.then((res) => setTemplateCount(res.totalElements))
			.catch(() => setTemplateCount(null));
	}, []);

	useEffect(() => {
		api
			.getRecoveryCards()
			.then((cards: RecoveryCardResponse[]) => setUnprocessedCount(cards.reduce((sum, c) => sum + c.comments.length, 0)))
			.catch(() => setUnprocessedCount(null));
	}, []);

	return (
		<div>
			<div className="home-greeting">
				{me?.profilePictureUrl ? (
					<img className="profile-avatar" src={me.profilePictureUrl} alt={me.username} />
				) : (
					<div className="profile-avatar profile-avatar-placeholder">{me?.username?.[0]?.toUpperCase() ?? "?"}</div>
				)}
				<h2>안녕하세요, {me?.username ?? "..."}님!</h2>
			</div>

			{insights.length > 0 && (
				<div className="insight-list">
					{insights.map((text) => (
						<p className="insight-item" key={text}>
							{text}
						</p>
					))}
				</div>
			)}

			<div className="summary-cards">
				<div className="summary-card">
					<div className="value">{summary?.contactedUsersThisMonth ?? "-"}</div>
					<div className="label">이번 달 컨택한 사용자 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{summary?.messagesSentThisMonth ?? "-"}</div>
					<div className="label">이번 달 발송한 메시지 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{templateCount ?? "-"}</div>
					<div className="label">전체 템플릿 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{unprocessedCount ?? "-"}</div>
					<div className="label">미처리 댓글 수</div>
				</div>
			</div>

			<div className="home-grid">
				<div className="chart-section">
					<div className="chart-header">
						<strong>템플릿 TOP 3</strong>
					</div>
					{topTemplates.length === 0 && <p className="hint">아직 발송 이력이 없습니다.</p>}
					{topTemplates.length > 0 && (
						<table className="data-table">
							<thead>
								<tr>
									<th>순번</th>
									<th>템플릿명</th>
									<th>메시지 발송수</th>
								</tr>
							</thead>
							<tbody>
								{topTemplates.map((t, index) => (
									<tr key={t.templateId}>
										<td>{index + 1}</td>
										<td>{t.templateName}</td>
										<td>{t.messagesSent}</td>
									</tr>
								))}
							</tbody>
						</table>
					)}
				</div>

				<div className="chart-section">
					<div className="chart-header">
						<strong>최근 발송</strong>
					</div>
					{recentLogs.length === 0 && <p className="hint">아직 발송 이력이 없습니다.</p>}
					{recentLogs.length > 0 && (
						<table className="data-table">
							<thead>
								<tr>
									<th>템플릿</th>
									<th>결과</th>
								</tr>
							</thead>
							<tbody>
								{recentLogs.map((log) => (
									<tr key={log.id}>
										<td>{log.templateName}</td>
										<td className={log.result === "SUCCESS" ? "success" : "failure"}>{log.result}</td>
									</tr>
								))}
							</tbody>
						</table>
					)}
				</div>
			</div>

			<div className="quick-link-grid">
				{QUICK_LINKS.map((link) => (
					<Link className="quick-link-card" to={link.to} key={link.to}>
						<strong>{link.label}</strong>
						<p>{link.desc}</p>
					</Link>
				))}
			</div>
		</div>
	);
}
