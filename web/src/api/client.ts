import type {
	AccountMeResponse,
	AccountSettingsResponse,
	AudienceType,
	ChartBucket,
	ChartGranularity,
	CreateTemplateRequest,
	MediaUploadResponse,
	PageResponse,
	PostResponse,
	RecoveryCardResponse,
	SendLogInsightResponse,
	SendLogResponse,
	SendLogSummaryResponse,
	TemplateDetailResponse,
	TemplateRankingResponse,
	TemplateResponse,
	UpdateAccountSettingsRequest,
} from "./types";

export async function checkAuthenticated(): Promise<boolean> {
	const res = await fetch("/api/account/me", { credentials: "same-origin" });
	return res.ok;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
	const headers = new Headers(options.headers);
	const isFormData = options.body instanceof FormData;
	if (!isFormData) headers.set("Content-Type", "application/json");

	const res = await fetch(path, { ...options, headers, credentials: "same-origin" });

	if (res.status === 401) {
		window.location.href = "/login";
		throw new Error("인증이 만료되었습니다");
	}

	if (!res.ok) {
		const body = await res.json().catch(() => ({}));
		throw new Error(body.message ?? `요청에 실패했습니다 (${res.status})`);
	}

	if (res.status === 204) return undefined as T;
	return res.json() as Promise<T>;
}

function toStringParams(params: Record<string, string | number | undefined>): Record<string, string> {
	const result: Record<string, string> = {};
	for (const [key, value] of Object.entries(params)) {
		if (value !== undefined) result[key] = String(value);
	}
	return result;
}

export const api = {
	getLoginUrl: () => request<{ url: string }>("/api/auth/instagram/login-url"),
	logout: () => request<void>("/api/auth/logout", { method: "POST" }),
	listTemplates: (page = 0, size = 10) =>
		request<PageResponse<TemplateResponse>>(`/api/templates?page=${page}&size=${size}`),
	createTemplate: (body: CreateTemplateRequest) =>
		request<TemplateResponse>("/api/templates", { method: "POST", body: JSON.stringify(body) }),
	getTemplate: (id: number) => request<TemplateDetailResponse>(`/api/templates/${id}`),
	updateTemplate: (id: number, body: CreateTemplateRequest) =>
		request<TemplateResponse>(`/api/templates/${id}`, { method: "PUT", body: JSON.stringify(body) }),
	deleteTemplate: (id: number) => request<void>(`/api/templates/${id}`, { method: "DELETE" }),
	updateTemplateActiveYn: (id: number, activeYn: boolean) =>
		request<TemplateResponse>(`/api/templates/${id}/active-yn`, {
			method: "PUT",
			body: JSON.stringify({ activeYn }),
		}),
	listSendLogs: (
		page = 0,
		size = 10,
		filters: { templateName?: string; audienceType?: AudienceType; from?: string; to?: string } = {},
	) => {
		const query = new URLSearchParams({ page: String(page), size: String(size), ...toStringParams(filters) });
		return request<PageResponse<SendLogResponse>>(`/api/send-logs?${query.toString()}`);
	},
	getSendLogSummary: () => request<SendLogSummaryResponse>("/api/send-logs/summary"),
	getSendLogChart: (
		granularity: ChartGranularity,
		params: { date?: string; from?: string; to?: string; year?: number } = {},
	) => {
		const query = new URLSearchParams({ granularity, ...toStringParams(params) });
		return request<ChartBucket[]>(`/api/send-logs/chart?${query.toString()}`);
	},
	getSendLogInsights: () => request<SendLogInsightResponse[]>("/api/send-logs/insights"),
	getTopTemplates: () => request<TemplateRankingResponse[]>("/api/send-logs/top-templates"),
	listPosts: () => request<PostResponse[]>("/api/posts"),
	uploadMedia: (file: File) => {
		const formData = new FormData();
		formData.append("file", file);
		return request<MediaUploadResponse>("/api/media", { method: "POST", body: formData });
	},
	getMe: () => request<AccountMeResponse>("/api/account/me"),
	getSettings: () => request<AccountSettingsResponse>("/api/settings"),
	updateSettings: (body: UpdateAccountSettingsRequest) =>
		request<AccountSettingsResponse>("/api/settings", { method: "PUT", body: JSON.stringify(body) }),
	getRecoveryCards: () => request<RecoveryCardResponse[]>("/api/recovery/cards"),
	processRecoveryComment: (postId: number, commentId: string) =>
		request<void>(`/api/recovery/posts/${postId}/comments/${commentId}/process`, { method: "POST" }),
	processRecoveryPostAll: (postId: number) =>
		request<void>(`/api/recovery/posts/${postId}/process-all`, { method: "POST" }),
};
