import { Route, Routes } from "react-router-dom"
import { Layout } from "./ui/Layout"
import { CalculateScreen } from "./ui/screens/CalculateScreen"
import { HistoryScreen } from "./ui/screens/HistoryScreen"
import { ResultScreen } from "./ui/screens/ResultScreen"
import { SettingsScreen } from "./ui/screens/SettingsScreen"
import { ROUTES } from "./ui/routes"

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path={ROUTES.calculate} element={<CalculateScreen />} />
        <Route path={ROUTES.resultPattern} element={<ResultScreen />} />
        <Route path={ROUTES.history} element={<HistoryScreen />} />
        <Route path={ROUTES.settings} element={<SettingsScreen />} />
      </Route>
    </Routes>
  )
}

export default App
