import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
	const { logout } = useAuth();

	return (
		<div>
			<nav className="navbar">
				<span className="brand">mySocial</span>
				<NavLink to="/templates" className={({ isActive }) => (isActive ? "active" : "")}>
					템플릿
				</NavLink>
				<NavLink to="/send-logs" className={({ isActive }) => (isActive ? "active" : "")}>
					발송 통계
				</NavLink>
				<button className="logout-button" onClick={logout}>
					로그아웃
				</button>
			</nav>
			<main className="content">
				<Outlet />
			</main>
		</div>
	);
}
