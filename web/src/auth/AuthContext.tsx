import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { clearToken, getToken, setToken as saveToken } from "../api/client";

interface AuthContextValue {
	isAuthenticated: boolean;
	login: (token: string) => void;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [isAuthenticated, setIsAuthenticated] = useState(() => getToken() !== null);

	const login = useCallback((token: string) => {
		saveToken(token);
		setIsAuthenticated(true);
	}, []);

	const logout = useCallback(() => {
		clearToken();
		setIsAuthenticated(false);
	}, []);

	return <AuthContext.Provider value={{ isAuthenticated, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
