import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
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

	// 계정 정보를 여기서 한 번만 가져와서 Layout/HomePage 등이 각자 다시 호출하지 않고 재사용한다.
	// (로그인 직후 여러 곳이 동시에 /api/account/me를 호출하면서 생기던 경합 문제의 원인이었다.)
	const refresh = useCallback(async (): Promise<boolean> => {
		const result = await fetchAccountMe();
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
