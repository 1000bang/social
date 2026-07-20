import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, checkAuthenticated } from "../api/client";

interface AuthContextValue {
	isAuthenticated: boolean | null;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

	useEffect(() => {
		checkAuthenticated().then(setIsAuthenticated);
	}, []);

	const logout = useCallback(() => {
		api.logout().finally(() => setIsAuthenticated(false));
	}, []);

	if (isAuthenticated === null) return <div className="centered-page">불러오는 중...</div>;

	return <AuthContext.Provider value={{ isAuthenticated, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
