import type {
	AccountSettingsResponse,
	ChartBucket,
	ChartGranularity,
	CreateTemplateRequest,
	MediaUploadResponse,
	PageResponse,
	PostResponse,
	SendLogResponse,
	SendLogSummaryResponse,
	TemplateDetailResponse,
	TemplateRankingResponse,
	TemplateResponse,
	UpdateAccountSettingsRequest,
} from "./types";

const TOKEN_KEY = "mysocial_jwt";

export function getToken(): string | null {
	return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
	localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
	localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
	const token = getToken();
	const headers = new Headers(options.headers);
	const isFormData = options.body instanceof FormData;
	if (!isFormData) headers.set("Content-Type", "application/json");
	if (token) headers.set("Authorization", `Bearer ${token}`);

	const res = await fetch(path, { ...options, headers });

	if (res.status === 401) {
		clearToken();
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

export const api = {
	listTemplates: (page = 0, size = 10) =>
		request<PageResponse<TemplateResponse>>(`/api/templates?page=${page}&size=${size}`),
	createTemplate: (body: CreateTemplateRequest) =>
		request<TemplateResponse>("/api/templates", { method: "POST", body: JSON.stringify(body) }),
	getTemplate: (id: number) => request<TemplateDetailResponse>(`/api/templates/${id}`),
	updateTemplate: (id: number, body: CreateTemplateRequest) =>
		request<TemplateResponse>(`/api/templates/${id}`, { method: "PUT", body: JSON.stringify(body) }),
	deleteTemplate: (id: number) => request<void>(`/api/templates/${id}`, { method: "DELETE" }),
	listSendLogs: (page = 0, size = 10) =>
		request<PageResponse<SendLogResponse>>(`/api/send-logs?page=${page}&size=${size}`),
	getSendLogSummary: () => request<SendLogSummaryResponse>("/api/send-logs/summary"),
	getSendLogChart: (granularity: ChartGranularity) =>
		request<ChartBucket[]>(`/api/send-logs/chart?granularity=${granularity}`),
	getTopTemplates: () => request<TemplateRankingResponse[]>("/api/send-logs/top-templates"),
	listPosts: () => request<PostResponse[]>("/api/posts"),
	uploadMedia: (file: File) => {
		const formData = new FormData();
		formData.append("file", file);
		return request<MediaUploadResponse>("/api/media", { method: "POST", body: formData });
	},
	getSettings: () => request<AccountSettingsResponse>("/api/settings"),
	updateSettings: (body: UpdateAccountSettingsRequest) =>
		request<AccountSettingsResponse>("/api/settings", { method: "PUT", body: JSON.stringify(body) }),
};
