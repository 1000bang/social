import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { AuthCallbackPage } from "./pages/AuthCallbackPage";
import { TemplatesPage } from "./pages/TemplatesPage";
import { SendLogsPage } from "./pages/SendLogsPage";
import { SettingsPage } from "./pages/SettingsPage";

export function App() {
	return (
		<AuthProvider>
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/auth/callback" element={<AuthCallbackPage />} />
				<Route element={<ProtectedRoute />}>
					<Route element={<Layout />}>
						<Route path="/templates" element={<TemplatesPage />} />
						<Route path="/send-logs" element={<SendLogsPage />} />
						<Route path="/settings" element={<SettingsPage />} />
					</Route>
				</Route>
				<Route path="*" element={<Navigate to="/templates" replace />} />
			</Routes>
		</AuthProvider>
	);
}
