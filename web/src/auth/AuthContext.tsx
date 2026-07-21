import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, checkAuthenticated } from "../api/client";

interface AuthContextValue {
	isAuthenticated: boolean | null;
	refresh: () => Promise<void>;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

	useEffect(() => {
		checkAuthenticated().then(setIsAuthenticated);
	}, []);

	// 팝업으로 로그인을 완료했을 때는 페이지가 새로고침되지 않아 이 값이 저절로 갱신되지 않는다.
	// 그래서 로그인 성공 후 명시적으로 다시 확인해야 한다.
	const refresh = useCallback(async () => {
		const authenticated = await checkAuthenticated();
		setIsAuthenticated(authenticated);
	}, []);

	const logout = useCallback(() => {
		api.logout().finally(() => setIsAuthenticated(false));
	}, []);

	if (isAuthenticated === null) return <div className="centered-page">불러오는 중...</div>;

	return <AuthContext.Provider value={{ isAuthenticated, refresh, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
