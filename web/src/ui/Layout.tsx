import { Link, Outlet, useLocation, useNavigate } from "react-router-dom"
import { IosInstallBanner } from "./IosInstallBanner"
import "./Layout.css"
import { ROUTES } from "./routes"

interface TopLevelDestination {
  path: string
  label: string
  icon: string
}

const TOP_LEVEL_DESTINATIONS: TopLevelDestination[] = [
  { path: ROUTES.calculate, label: "Розрахунок", icon: "🧮" },
  { path: ROUTES.history, label: "Історія", icon: "🕘" },
  { path: ROUTES.settings, label: "Налаштування", icon: "⚙️" },
]

function titleForPath(pathname: string): string {
  if (pathname === ROUTES.calculate) return "VetDose"
  if (pathname === ROUTES.history) return "Історія розрахунків"
  if (pathname === ROUTES.settings) return "Налаштування"
  if (pathname.startsWith("/result/")) return "Результат розрахунку"
  return "VetDose"
}

export function Layout() {
  const location = useLocation()
  const navigate = useNavigate()
  const isTopLevel = TOP_LEVEL_DESTINATIONS.some((d) => d.path === location.pathname)

  return (
    <div className="app-shell">
      <header className="app-header">
        {!isTopLevel ? (
          <button type="button" className="app-header__back" onClick={() => navigate(-1)} aria-label="Назад">
            ←
          </button>
        ) : (
          <span className="app-header__logo-icon">💉</span>
        )}
        <h1>{titleForPath(location.pathname)}</h1>
      </header>

      <IosInstallBanner />

      <main className="app-content">
        <Outlet />
      </main>

      {isTopLevel && (
        <nav className="bottom-nav">
          {TOP_LEVEL_DESTINATIONS.map((d) => (
            <Link
              key={d.path}
              to={d.path}
              className={`bottom-nav__item${location.pathname === d.path ? " bottom-nav__item--active" : ""}`}
            >
              <span aria-hidden="true">{d.icon}</span>
              <span>{d.label}</span>
            </Link>
          ))}
        </nav>
      )}
    </div>
  )
}
