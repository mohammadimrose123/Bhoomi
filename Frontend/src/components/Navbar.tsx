import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { ArrowUpRight, Menu, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const links = [
  { label: 'Home', to: '/' },
  { label: 'Explore lands', to: '/explore' },
  { label: 'Categories', to: '/#categories' },
  { label: 'About us', to: '/#intelligence' },
  { label: 'Contact', to: '/#contact' },
]

function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, user, signOut } = useAuth()

  useEffect(() => {
    if (!location.hash) return

    const element = document.getElementById(location.hash.slice(1))

    if (!element) return

    requestAnimationFrame(() => {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }, [location])

  const handleLogout = () => {
    signOut()
    navigate('/')
    setMenuOpen(false)
  }

  return (
    <header className="site-header" id="top">
      <div className="header-inner">
        <Link className="brand" to="/" aria-label="Bhoomi home" onClick={() => setMenuOpen(false)}>
          <span className="brand-mark" aria-hidden="true">B</span>
          <span className="brand-copy">
            <span className="brand-name">bhoomi<span>.</span></span>
            <span className="brand-caption">LAND, WITH CONTEXT</span>
          </span>
        </Link>

        <nav className="desktop-nav" aria-label="Main navigation">
          {links.map((link) =>
            link.to.startsWith('/#') ? (
              <Link key={link.label} to={link.to} onClick={() => setMenuOpen(false)}>
                {link.label}
              </Link>
            ) : (
              <NavLink
                key={link.label}
                to={link.to}
                className={({ isActive }) => (isActive ? 'active' : undefined)}
                end={link.to === '/'}
                onClick={() => setMenuOpen(false)}
              >
                {link.label}
              </NavLink>
            ),
          )}
        </nav>

        <div className="header-actions">
          {isAuthenticated ? (
            <>
              {user?.role === 'BUYER' && <Link className="login-link" to="/favorites">Favorites</Link>}
              {user?.role === 'BROKER' && <Link className="login-link" to="/broker">Studio</Link>}
              {user?.role === 'ADMIN' && <Link className="login-link" to="/admin">Admin</Link>}
              <Link className="login-link" to="/dashboard">{user?.fullName ?? 'Dashboard'}</Link>
              <button type="button" className="register-link" onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <Link className="login-link" to="/login">Log in</Link>
              <Link className="register-link" to="/register">Register</Link>
            </>
          )}
          <Link className="button button-dark button-small" to={isAuthenticated ? '/dashboard' : '/register'}>
            {isAuthenticated ? 'My account' : 'List your land'} <ArrowUpRight size={15} />
          </Link>
        </div>

        <button
          className="mobile-menu-toggle"
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-expanded={menuOpen}
        >
          {menuOpen ? <X size={21} /> : <Menu size={21} />}
        </button>
      </div>

      {menuOpen && (
        <nav className="mobile-nav" aria-label="Mobile navigation">
          {links.map((link) => (
            <Link key={link.label} to={link.to} onClick={() => setMenuOpen(false)}>
              {link.label}
            </Link>
          ))}
          {isAuthenticated ? (
            <>
              {user?.role === 'BUYER' && <Link to="/favorites" onClick={() => setMenuOpen(false)}>Favorites & activity</Link>}
              {user?.role === 'BROKER' && <Link to="/broker" onClick={() => setMenuOpen(false)}>Broker studio</Link>}
              {user?.role === 'ADMIN' && <Link to="/admin" onClick={() => setMenuOpen(false)}>Admin control center</Link>}
              <Link to="/dashboard" onClick={() => setMenuOpen(false)}>Dashboard</Link>
              <button type="button" className="register-link" onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <Link to="/login" onClick={() => setMenuOpen(false)}>Log in</Link>
              <Link to="/register" onClick={() => setMenuOpen(false)}>Register</Link>
            </>
          )}
          <Link className="button button-dark" to={isAuthenticated ? '/dashboard' : '/register'} onClick={() => setMenuOpen(false)}>
            {isAuthenticated ? 'My account' : 'List your land'} <ArrowUpRight size={15} />
          </Link>
        </nav>
      )}
    </header>
  )
}

export default Navbar
