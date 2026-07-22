import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, fetchAccountMe } from "../api/client";
import type { AccountMeResponse } from "../api/types";

interface AuthContextValue {
	isAuthenticated: boolean | null;
	me: AccountMeResponse | null;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
	const [me, setMe] = useState<AccountMeResponse | null>(null);

	// 계정 정보를 여기서 한 번만 가져와서 Layout/HomePage 등이 각자 다시 호출하지 않고 재사용한다.
	useEffect(() => {
		fetchAccountMe().then((result) => {
			setMe(result);
			setIsAuthenticated(result !== null);
		});
	}, []);

	const logout = useCallback(() => {
		api.logout().finally(() => {
			setMe(null);
			setIsAuthenticated(false);
		});
	}, []);

	if (isAuthenticated === null) return <div className="centered-page">불러오는 중...</div>;

	return <AuthContext.Provider value={{ isAuthenticated, me, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
