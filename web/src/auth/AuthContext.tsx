import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { api, fetchAccountMe } from "../api/client";
import type { AccountMeResponse } from "../api/types";

interface AuthContextValue {
	isAuthenticated: boolean | null;
	me: AccountMeResponse | null;
	refresh: () => Promise<boolean>;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
	const [me, setMe] = useState<AccountMeResponse | null>(null);
	const requestIdRef = useRef(0);

	// 계정 정보를 여기서 한 번만 가져와서 Layout/HomePage 등이 각자 다시 호출하지 않고 재사용한다.
	// (로그인 직후 여러 곳이 동시에 /api/account/me를 호출하면서 생기던 경합 문제의 원인이었다.)
	//
	// 모바일 브라우저가 백그라운드 탭의 요청 처리를 늦추는 경우가 있어서, 마운트 시점에 시작한
	// (로그인 전) 확인 요청이 팝업 로그인이 끝난 뒤에야 뒤늦게 응답으로 도착할 수 있다. 이때 먼저
	// 시작한 요청이 나중에 끝난 요청의 결과를 덮어써버리면 방금 성공한 로그인이 무효화된다.
	// 그래서 가장 최근에 시작한 요청의 결과만 반영하고, 그보다 오래된 요청의 결과는 버린다.
	const refresh = useCallback(async (): Promise<boolean> => {
		const requestId = ++requestIdRef.current;
		const result = await fetchAccountMe();
		if (requestId !== requestIdRef.current) return result !== null;
		setMe(result);
		setIsAuthenticated(result !== null);
		return result !== null;
	}, []);

	useEffect(() => {
		refresh();
	}, [refresh]);

	const logout = useCallback(() => {
		api.logout().finally(() => {
			setMe(null);
			setIsAuthenticated(false);
		});
	}, []);

	if (isAuthenticated === null) return <div className="centered-page">불러오는 중...</div>;

	return <AuthContext.Provider value={{ isAuthenticated, me, refresh, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
