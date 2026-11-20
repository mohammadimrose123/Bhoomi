import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import MainLayout from './layouts/MainLayout'
import AccountPage from './pages/AccountPage'
import BuyerPortalPage from './pages/BuyerPortalPage'
import BrokerStudioPage from './pages/BrokerStudioPage'
import AdminControlPage from './pages/AdminControlPage'
import DashboardPage from './pages/DashboardPage'
import ExploreLands from './pages/ExploreLands'
import Home from './pages/Home'
import LandDetails from './pages/LandDetails'
import LegalNotice from './pages/LegalNotice'
import NotFound from './pages/NotFound'
import PasswordRecoveryPage from './pages/PasswordRecoveryPage'

const router = createBrowserRouter([
  {
    element: <MainLayout />,
    children: [
      { path: '/', element: <Home /> },
      { path: '/explore', element: <ExploreLands /> },
      { path: '/lands/:id', element: <LandDetails /> },
      { path: '/login', element: <AccountPage /> },
      { path: '/register', element: <AccountPage /> },
      { path: '/forgot-password', element: <PasswordRecoveryPage /> },
      { path: '/reset-password', element: <PasswordRecoveryPage /> },
      { path: '/dashboard', element: <ProtectedRoute><DashboardPage /></ProtectedRoute> },
      { path: '/account', element: <ProtectedRoute allowedRoles={['BUYER']}><BuyerPortalPage /></ProtectedRoute> },
      { path: '/favorites', element: <ProtectedRoute allowedRoles={['BUYER']}><BuyerPortalPage /></ProtectedRoute> },
      { path: '/broker', element: <ProtectedRoute allowedRoles={['BROKER']}><BrokerStudioPage /></ProtectedRoute> },
      { path: '/admin', element: <ProtectedRoute allowedRoles={['ADMIN']}><AdminControlPage /></ProtectedRoute> },
      { path: '/privacy', element: <LegalNotice /> },
      { path: '/terms', element: <LegalNotice /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
}

export default App
