import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Sprout } from 'lucide-react'
import { authApi } from '../services/api'
import { useAuth } from '../context/AuthContext'

function AccountPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, signIn } = useAuth()
  const isRegister = location.pathname === '/register'

  const [form, setForm] = useState({
    email: '',
    password: '',
    fullName: '',
    phone: '',
    accountType: 'BUYER' as 'BUYER' | 'BROKER',
    businessName: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const handleChange = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      if (isRegister) {
        const response = await authApi.register({
          email: form.email,
          phone: form.phone || undefined,
          fullName: form.fullName,
          password: form.password,
          accountType: form.accountType,
          businessName: form.businessName || undefined,
        })
        signIn(response.data)
        setSuccess('Registration successful. Redirecting to your dashboard…')
        navigate('/dashboard', { replace: true })
        return
      }

      const response = await authApi.login({ email: form.email, password: form.password })
      signIn(response.data)
      setSuccess('Signed in successfully. Redirecting…')
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      const message = requestError instanceof Error && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail
          ?? (requestError as { response?: { data?: { message?: string } } }).response?.data?.message
          ?? 'Unable to complete the request. Please try again.'
        : 'Unable to complete the request. Please try again.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="account-page">
      <div className="account-card">
        <Link to="/" className="account-back"><ArrowLeft size={15} /> Back to Bhoomi</Link>
        <div className="account-mark"><Sprout size={22} /></div>
        <p className="eyebrow">{isRegister ? 'Join Bhoomi' : 'Welcome back'}</p>
        <h1>{isRegister ? 'Create your account.' : 'Sign in to continue.'}</h1>
        <p className="account-description">
          {isRegister
            ? 'Create a buyer or broker profile. Only real backend accounts are accepted.'
            : 'Use your Bhoomi account to access private listings, saved land, and site-visit requests.'}
        </p>

        {error && <div className="account-error" role="alert">{error}</div>}
        {success && <div className="account-success" role="status">{success}</div>}

        <form className="account-form" onSubmit={submit}>
          {isRegister && (
            <label>
              I am joining as
              <select required value={form.accountType} onChange={(event) => handleChange('accountType', event.target.value)}>
                <option value="BUYER">Buyer</option>
                <option value="BROKER">Broker</option>
              </select>
            </label>
          )}

          {isRegister && (
            <label>
              Full name
              <input
                type="text"
                placeholder="Your full name"
                value={form.fullName}
                onChange={(event) => handleChange('fullName', event.target.value)}
                autoComplete="name"
                required
              />
            </label>
          )}

          <label>
            Email address
            <input
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              value={form.email}
              onChange={(event) => handleChange('email', event.target.value)}
              required
            />
          </label>

          {isRegister && (
            <label>
              Phone number
              <input
                type="tel"
                placeholder="+91 98765 43210"
                autoComplete="tel"
                value={form.phone}
                onChange={(event) => handleChange('phone', event.target.value)}
              />
            </label>
          )}

          {isRegister && form.accountType === 'BROKER' && (
            <label>
              Business name
              <input
                type="text"
                placeholder="Brokerage or business name"
                value={form.businessName}
                onChange={(event) => handleChange('businessName', event.target.value)}
                required
              />
            </label>
          )}

          {!isRegister && (
            <label>
              Password
              <input
                type="password"
                autoComplete="current-password"
                value={form.password}
                onChange={(event) => handleChange('password', event.target.value)}
                required
              />
            </label>
          )}
          {!isRegister && <Link className="account-forgot" to="/forgot-password">Forgot password?</Link>}

          {isRegister && (
            <label>
              Password
              <input
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={(event) => handleChange('password', event.target.value)}
                required
                minLength={12}
              />
            </label>
          )}

          <button className="button button-dark account-submit" type="submit" disabled={loading}>
            {loading ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'} <ArrowRight size={16} />
          </button>
        </form>

        <div className="account-switch">
          {isRegister ? <>Already have an account? <Link to="/login">Log in</Link></> : <>New to Bhoomi? <Link to="/register">Register</Link></>}
        </div>
      </div>
    </section>
  )
}

export default AccountPage
