import type { CreateTemplateRequest, SendLogResponse, TemplateResponse } from "./types";

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
	headers.set("Content-Type", "application/json");
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
	getLoginUrl: () => request<{ url: string }>("/api/auth/instagram/login-url"),
	listTemplates: () => request<TemplateResponse[]>("/api/templates"),
	createTemplate: (body: CreateTemplateRequest) =>
		request<TemplateResponse>("/api/templates", { method: "POST", body: JSON.stringify(body) }),
	deleteTemplate: (id: number) => request<void>(`/api/templates/${id}`, { method: "DELETE" }),
	listSendLogs: () => request<SendLogResponse[]>("/api/send-logs"),
};
