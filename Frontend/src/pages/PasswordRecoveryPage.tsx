import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Eye, EyeOff, Sprout } from 'lucide-react'
import { authApi } from '../services/api'
import { getPasswordRequirements, passwordsMatch as checkPasswordsMatch } from '../utils/passwordValidation'

function getErrorMessage(error: unknown) {
  if (error instanceof Error && 'response' in error) {
    const response = (error as { response?: { status?: number; data?: { detail?: string } } }).response
    if (response?.status === 400 && response.data?.detail?.toLowerCase().includes('reset link')) {
      return 'This reset link is invalid, expired, or has already been used. Request a new one to continue.'
    }
    if (response?.status === 503) {
      return 'Password recovery is temporarily unavailable because email delivery is not configured.'
    }
    if (response?.status === 429) {
      return 'Too many recovery attempts. Please wait a while before trying again.'
    }
  }
  return 'Unable to complete the request. Please try again.'
}

function PasswordRecoveryPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const isReset = location.pathname === '/reset-password'
  const [token] = useState(() => new URLSearchParams(location.search).get('token') ?? '')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState(false)
  const [reset, setReset] = useState(false)
  const [expired, setExpired] = useState(isReset && !token)

  useEffect(() => {
    if (isReset && location.search) {
      navigate(location.pathname, { replace: true })
    }
  }, [isReset, location.pathname, location.search, navigate])

  const passwordChecks = useMemo(() => {
    const labels = [
      'At least 12 characters',
      'Lowercase and uppercase letters',
      'At least one number',
      'At least one special character',
    ]
    return getPasswordRequirements(password).map((valid, index) => ({ label: labels[index], valid }))
  }, [password])
  const strength = passwordChecks.filter((check) => check.valid).length
  const passwordsMatchValue = checkPasswordsMatch(password, confirmPassword)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    if (isReset && password !== confirmPassword) {
      setError('The passwords do not match.')
      return
    }
    if (isReset && strength !== passwordChecks.length) {
      setError('Choose a stronger password that meets all the requirements below.')
      return
    }

    setLoading(true)
    try {
      if (isReset) {
        await authApi.resetPassword(token, password)
        setReset(true)
      } else {
        await authApi.forgotPassword(email)
        setSent(true)
      }
    } catch (requestError) {
      const message = getErrorMessage(requestError)
      if (isReset && message.startsWith('This reset link')) {
        setExpired(true)
      } else {
        setError(message)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="account-page">
      <div className="account-card">
        <Link to="/login" className="account-back"><ArrowLeft size={15} /> Back to sign in</Link>
        <div className="account-mark"><Sprout size={22} /></div>
        <p className="eyebrow">{isReset ? 'Secure your account' : 'Account recovery'}</p>
        <h1>{isReset ? 'Choose a new password.' : 'Forgot your password?'}</h1>
        <p className="account-description">
          {isReset
            ? 'Create a strong password for your Bhoomi account.'
            : 'Enter the email address associated with your account. If it is registered, we will send a reset link.'}
        </p>

        {error && <div className="account-error" role="alert">{error}</div>}

        {sent && (
          <div className="account-success" role="status">
            <strong>Check your inbox</strong>
            <span>If the address is associated with an account and email delivery succeeds, a reset link will be sent.</span>
          </div>
        )}

        {reset && (
          <div className="account-success" role="status">
            <strong>Password updated</strong>
            <span>Your password has been changed. Sign in with your new password to continue.</span>
            <Link className="button button-dark account-submit" to="/login">Back to sign in <ArrowRight size={16} /></Link>
          </div>
        )}

        {expired && !reset && (
          <div className="account-error" role="alert">
            This reset link is missing, invalid, expired, or already used. <Link to="/forgot-password">Request a new link</Link>.
          </div>
        )}

        {!sent && !reset && !expired && (
          <form className="account-form" onSubmit={submit}>
            {!isReset ? (
              <label>
                Email address
                <input
                  type="email"
                  placeholder="you@example.com"
                  autoComplete="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </label>
            ) : (
              <>
                <label>
                  New password
                  <span className="password-field">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      minLength={12}
                      maxLength={72}
                      required
                    />
                    <button className="password-toggle" type="button" onClick={() => setShowPassword((shown) => !shown)}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}>
                      {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                    </button>
                  </span>
                </label>
                <div className="password-feedback" aria-live="polite">
                  <div className="password-strength" aria-label={`Password strength: ${strength} of 4`}>
                    {passwordChecks.map((check) => (
                      <span key={check.label} className={check.valid ? 'is-valid' : ''} />
                    ))}
                  </div>
                  <ul>
                    {passwordChecks.map((check) => (
                      <li key={check.label} className={check.valid ? 'is-valid' : ''}>{check.label}</li>
                    ))}
                  </ul>
                </div>
                <label>
                  Confirm new password
                  <span className="password-field">
                    <input
                      type={showConfirmation ? 'text' : 'password'}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      required
                      aria-invalid={confirmPassword.length > 0 && !passwordsMatchValue}
                    />
                    <button className="password-toggle" type="button"
                      onClick={() => setShowConfirmation((shown) => !shown)}
                      aria-label={showConfirmation ? 'Hide confirmation password' : 'Show confirmation password'}>
                      {showConfirmation ? <EyeOff size={17} /> : <Eye size={17} />}
                    </button>
                  </span>
                  {confirmPassword.length > 0 && (
                    <span className={passwordsMatchValue ? 'password-match is-valid' : 'password-match'}>
                      {passwordsMatchValue ? 'Passwords match' : 'Passwords do not match'}
                    </span>
                  )}
                </label>
              </>
            )}
            <button className="button button-dark account-submit" type="submit"
              disabled={loading || (isReset && !token)}>
              {loading ? 'Please wait…' : isReset ? 'Reset password' : 'Send reset link'} <ArrowRight size={16} />
            </button>
          </form>
        )}

        {!reset && (
          <div className="account-switch">
            Remember your password? <Link to="/login">Sign in</Link>
          </div>
        )}
      </div>
    </section>
  )
}

export default PasswordRecoveryPage
