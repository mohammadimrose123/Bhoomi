export function getPasswordRequirements(password: string) {
  return [
    password.length >= 12,
    /[a-z]/.test(password) && /[A-Z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ]
}

export function passwordsMatch(password: string, confirmation: string) {
  return confirmation.length > 0 && password === confirmation
}
